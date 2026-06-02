package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.ProviderCredentials;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderCallbackFinalizer {

    private final PaymentRepository paymentRepository;
    private final MerchantProviderAccountService providerAccountService;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;
    private final WebhookService webhookService;
    private final LogEntryRepository logEntryRepository;

    @Async
    @Transactional
    public void finalizePayDunyaCheckoutIpn(String token, String formStatus, String rawForm) {
        paymentRepository.findByProviderPaymentIdForUpdate(token).ifPresentOrElse(payment -> {
            logCallback(payment, "PAYDUNYA_CALLBACK_RECEIVED", LogStatus.PENDING, rawForm, null, false);
            if (isTerminal(payment.getStatus())) {
                log.info("Ignoring PayDunya IPN for payment={} because status is already {}", payment.getPaymentId(), payment.getStatus());
                return;
            }
            PaymentStatus verifiedStatus = payDunyaClient.checkStatus(token, credentialsFor(payment));
            if (verifiedStatus == PaymentStatus.PENDING_RECONCILIATION && formStatus != null) {
                verifiedStatus = statusFromPayDunyaForm(formStatus);
            }
            applyProviderStatus(payment, verifiedStatus, "PAYDUNYA_CALLBACK");
            logCallback(payment, "PAYDUNYA_CALLBACK_FINALIZED", statusToLogStatus(verifiedStatus), rawForm, null, false);
        }, () -> log.warn("PayDunya finalizer could not find payment for token={}", token));
    }

    @Async
    @Transactional
    public void finalizeKkiapayCheckoutIpn(KkiapayCallbackDTO callback, String rawBody) {
        paymentRepository.findByProviderPaymentIdForUpdate(callback.getTransactionId()).ifPresentOrElse(payment -> {
            logCallback(payment, "KKIAPAY_CALLBACK_RECEIVED", LogStatus.PENDING, rawBody, null, false);
            if (isTerminal(payment.getStatus())) {
                log.info("Ignoring KkiaPay IPN for payment={} because status is already {}", payment.getPaymentId(), payment.getStatus());
                return;
            }
            PaymentStatus verifiedStatus = kkiapayClient.checkStatus(callback.getTransactionId(), credentialsFor(payment));
            if (verifiedStatus == PaymentStatus.PENDING_RECONCILIATION) {
                verifiedStatus = callback.isPaymentSucces() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            }
            applyProviderStatus(payment, verifiedStatus, "KKIAPAY_CALLBACK");
            logCallback(payment, "KKIAPAY_CALLBACK_FINALIZED", statusToLogStatus(verifiedStatus), rawBody, null, false);
        }, () -> log.warn("KkiaPay finalizer could not find payment for transactionId={}", callback.getTransactionId()));
    }

    private ProviderCredentials credentialsFor(Payment payment) {
        if (payment.getMerchantProviderAccountId() == null) {
            return null;
        }
        return providerAccountService.decrypt(providerAccountService.getAccount(payment.getMerchantProviderAccountId()));
    }

    private PaymentStatus statusFromPayDunyaForm(String value) {
        return switch (value.trim().toUpperCase()) {
            case "COMPLETED", "SUCCESS", "SUCCEEDED" -> PaymentStatus.SUCCESS;
            case "CANCELLED", "CANCELED" -> PaymentStatus.CANCELLED;
            case "FAILED", "ERROR" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private void applyProviderStatus(Payment payment, PaymentStatus status, String source) {
        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());
        if (status == PaymentStatus.SUCCESS) {
            payment.setFailureReason(null);
        } else if (status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED) {
            payment.setFailureReason(source + "_" + status.name());
        }
        paymentRepository.save(payment);
        if (isTerminal(status)) {
            webhookService.sendWebhook(payment);
        }
    }

    private boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS || status == PaymentStatus.FAILED || status == PaymentStatus.CANCELLED;
    }

    private LogStatus statusToLogStatus(PaymentStatus status) {
        return switch (status) {
            case SUCCESS -> LogStatus.SUCCESS;
            case FAILED, CANCELLED -> LogStatus.FAILED;
            default -> LogStatus.PENDING;
        };
    }

    private void logCallback(Payment payment, String event, LogStatus status, String message, ErrorType errorType, boolean fallbackUsed) {
        LogEntry entry = new LogEntry();
        entry.setPaymentId(payment.getPaymentId());
        entry.setRouteUsed(nonBlank(payment.getRouteName(), nonBlank(payment.getProvider(), "UNKNOWN")));
        entry.setProvider(nonBlank(payment.getProvider(), "UNKNOWN"));
        entry.setResponseTime(payment.getProviderResponseTimeMs() == null ? 0.0 : payment.getProviderResponseTimeMs().doubleValue());
        entry.setStatus(status);
        entry.setFailureReason(event);
        entry.setErrorType(errorType);
        entry.setFallbackUsed(fallbackUsed);
        entry.setMessage(truncate(message));
        logEntryRepository.save(entry);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 5000 ? message.substring(0, 4990) + "...[TRUNCATED]" : message;
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
