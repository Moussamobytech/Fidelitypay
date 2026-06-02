package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.ProviderCredentials;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderCallbackService {

    private final PaymentRepository paymentRepository;
    private final MerchantProviderAccountService providerAccountService;
    private final PayDunyaClient payDunyaClient;
    private final ProviderCallbackFinalizer finalizer;

    @Transactional(readOnly = true)
    public String acceptPayDunyaCheckoutIpn(String token, String status, String hash, String rawForm) {
        if (token == null || token.isBlank() || hash == null || hash.isBlank()) {
            log.warn("Ignored PayDunya IPN because token or hash is missing");
            return "ignored";
        }
        Payment payment = paymentRepository.findByProviderPaymentId(token).orElse(null);
        if (payment == null) {
            log.warn("Ignored PayDunya IPN because token={} is unknown", token);
            return "ignored";
        }
        ProviderCredentials credentials = credentialsFor(payment);
        if (!payDunyaClient.isValidCallbackHash(hash, credentials)) {
            log.warn("Ignored PayDunya IPN for payment={} because hash is invalid", payment.getPaymentId());
            return "ignored";
        }
        log.info("Accepted PayDunya IPN payment={} token={} status={}", payment.getPaymentId(), token, status);
        finalizer.finalizePayDunyaCheckoutIpn(token, status, rawForm);
        return "ok";
    }

    @Transactional(readOnly = true)
    public String acceptKkiapayCheckoutIpn(KkiapayCallbackDTO callback, String rawBody) {
        if (callback == null || callback.getTransactionId() == null || callback.getTransactionId().isBlank()) {
            log.warn("Ignored KkiaPay IPN because transaction id is missing");
            return "ignored";
        }
        Payment payment = paymentRepository.findByProviderPaymentId(callback.getTransactionId()).orElse(null);
        if (payment == null) {
            log.warn("Ignored KkiaPay IPN because transactionId={} is unknown", callback.getTransactionId());
            return "ignored";
        }
        log.info("Accepted KkiaPay IPN payment={} transactionId={} event={} success={}", payment.getPaymentId(),
                callback.getTransactionId(), callback.getEvent(), callback.isPaymentSucces());
        finalizer.finalizeKkiapayCheckoutIpn(callback, rawBody);
        return "ok";
    }

    private ProviderCredentials credentialsFor(Payment payment) {
        if (payment.getMerchantProviderAccountId() == null) {
            return null;
        }
        return providerAccountService.decrypt(providerAccountService.getAccount(payment.getMerchantProviderAccountId()));
    }
}
