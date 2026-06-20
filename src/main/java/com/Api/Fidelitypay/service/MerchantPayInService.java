package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.MerchantApiPrincipal;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentRequest;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentResponse;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.PayInProviderClient;
import com.Api.Fidelitypay.integration.PayInProviderRequest;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.integration.ProviderCredentials;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.MerchantProviderAccount;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantPayInService {

    private final PaymentRepository paymentRepository;
    private final LogEntryRepository logEntryRepository;
    private final PaymentRouteService routeService;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;
    private final WebhookService webhookService;
    private final MerchantProviderAccountService providerAccountService;
    private final PaymentStateTransitionService transitionService;

    @Value("${payment.providers.allow-global-credentials-fallback:false}")
    private boolean allowGlobalCredentialsFallback;

    @Transactional
    public MerchantPaymentResponse initiate(MerchantApiPrincipal principal, MerchantPaymentRequest request,
            String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        String normalizedCountry = normalize(request.getCountry());
        String normalizedOperator = normalizeOperator(request.getOperator());
        if (requiresPhone(normalizedCountry, normalizedOperator)
                && (request.getCustomer().getPhone() == null || request.getCustomer().getPhone().isBlank())) {
            throw new IllegalArgumentException("customer.phone is required for mobile-money payments");
        }
        String environment = normalize(principal.getEnvironment());

        return paymentRepository.findByApiKeyIdAndIdempotencyKey(principal.getApiKey().getId(), idempotencyKey)
                .map(this::toResponse)
                .orElseGet(() -> createAndInitiate(principal, request, idempotencyKey, normalizedCountry,
                        normalizedOperator, environment));
    }

    @Transactional(readOnly = true)
    public MerchantPaymentResponse getPayment(MerchantApiPrincipal principal, String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        assertOwner(principal, payment);
        return toResponse(payment);
    }

    @Transactional
    public MerchantPaymentResponse submitOtp(MerchantApiPrincipal principal, String paymentId, String otp) {
        Payment payment = paymentRepository.findByPaymentIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        assertOwner(principal, payment);
        if (payment.getStatus() != PaymentStatus.REQUIRES_ACTION
                || !"SUBMIT_OTP".equalsIgnoreCase(payment.getNextActionType())) {
            throw new IllegalStateException("Payment does not require OTP");
        }

        PaymentResult result = clientFor(payment.getProvider()).validateAction(payment, "SUBMIT_OTP", otp, credentialsFor(payment));
        payment.setProviderResponse(result.getRawResponse());
        payment.setProviderResponseTimeMs((long) result.getResponseTimeMs());
        payment.setUpdatedAt(LocalDateTime.now());
        if (result.isSuccess()) {
            transitionService.transition(payment, PaymentStatus.PENDING, "OTP_ACTION");
        } else {
            PaymentStateTransitionService.TransitionResult transition =
                    transitionService.transition(payment, PaymentStatus.FAILED, "OTP_ACTION");
            payment.setFailureReason(determineFailureReason(result));
            if (transitionService.shouldNotifyWebhook(transition)) {
                webhookService.sendWebhook(payment);
            }
        }
        paymentRepository.save(payment);
        return toResponse(payment);
    }

    private MerchantPaymentResponse createAndInitiate(MerchantApiPrincipal principal, MerchantPaymentRequest request,
            String idempotencyKey, String country, String operator, String environment) {
        Payment payment = new Payment();
        payment.setPaymentId("fp_" + UUID.randomUUID());
        payment.setUser(principal.getUser());
        payment.setApiKeyId(principal.getApiKey().getId());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setInitiationSource(defaultInitiationSource(principal.getInitiationSource()));
        payment.setAmount(BigDecimal.valueOf(request.getAmount()));
        payment.setCurrency("XOF");
        payment.setCountry(country);
        payment.setOperator(operator);
        payment.setCustomerPhone(request.getCustomer().getPhone());
        payment.setCustomerFirstname(request.getCustomer().getFirstname());
        payment.setCustomerLastname(request.getCustomer().getLastname());
        payment.setCustomerEmail(request.getCustomer().getEmail());
        payment.setReturnUrl(request.getReturnUrl());
        payment.setCancelUrl(request.getCancelUrl());
        payment.setCost(BigDecimal.ZERO);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        List<PaymentProviderRoute> routes = routeService.findAvailablePayIn(country, operator, environment, principal.getUser().getId());
        if (routes.isEmpty()) {
            PaymentStateTransitionService.TransitionResult transition =
                    transitionService.transition(payment, PaymentStatus.FAILED, "PAYIN_INITIATE");
            payment.setFailureReason(routeService.hasAnyPayInRoute(country, operator)
                    ? "NO_PROVIDER_AVAILABLE_FOR_ENVIRONMENT"
                    : "UNSUPPORTED_PAYIN_CAPABILITY");
            paymentRepository.save(payment);
            if (transitionService.shouldNotifyWebhook(transition)) {
                webhookService.sendWebhook(payment);
            }
            return toResponse(payment);
        }

        PaymentResult finalResult = null;
        PaymentProviderRoute finalRoute = null;
        int attempt = 0;
        for (PaymentProviderRoute route : routes) {
            attempt++;
            MerchantProviderAccount account = providerAccountService.getEnabledAccount(principal.getUser().getId(),
                    route.getProvider().getId(), environment);
            if (account == null && !allowGlobalCredentialsFallback) {
                continue;
            }
            ProviderCredentials credentials = account == null ? null : providerAccountService.decrypt(account);
            PaymentResult result = clientFor(route.getProvider().getCode()).initiatePayIn(
                    toProviderRequest(payment, route, credentials, account == null ? environment : account.getEnvironment()));
            saveRouteAttempt(payment, route, result, attempt > 1);
            finalResult = result;
            finalRoute = route;
            payment.setAttemptCount(attempt);
            payment.setProvider(route.getProvider().getCode());
            payment.setMerchantProviderAccountId(account == null ? null : account.getId());
            payment.setRouteName(routeName(route));
            payment.setFlowType(route.getFlowType().name());
            payment.setProviderChannel(route.getProviderChannel());
            payment.setProviderResponse(result.getRawResponse());
            payment.setProviderResponseTimeMs((long) result.getResponseTimeMs());
            payment.setErrorType(result.getErrorType());

            if (result.isSuccess()) {
                payment.setProviderPaymentId(result.getProviderId());
                payment.setPaymentUrl(result.getPaymentUrl());
                payment.setRouteHealth("HEALTHY");
                payment.setUsedFallback(attempt > 1);
                payment.setFallbackReason(attempt > 1 ? "PRIMARY_PROVIDER_FAILED" : null);
                if (result.isRequiresAction()) {
                    payment.setNextActionType(result.getNextActionType());
                    PaymentStateTransitionService.TransitionResult transition =
                            transitionService.transition(payment, PaymentStatus.REQUIRES_ACTION, "PAYIN_INITIATE");
                    if (transitionService.shouldNotifyWebhook(transition)) {
                        webhookService.sendWebhook(payment);
                    }
                } else {
                    transitionService.transition(payment, PaymentStatus.PENDING, "PAYIN_INITIATE");
                }
                payment.setFailureReason(null);
                paymentRepository.save(payment);
                return toResponse(payment);
            }

            if (result.isProviderTransactionCreated()) {
                payment.setProviderPaymentId(result.getProviderId());
                transitionService.transition(payment, PaymentStatus.PENDING_RECONCILIATION, "PAYIN_INITIATE");
                payment.setFailureReason("PROVIDER_RESULT_UNKNOWN");
                paymentRepository.save(payment);
                return toResponse(payment);
            }

            if (!shouldFallback(result)) {
                break;
            }
        }

        PaymentStateTransitionService.TransitionResult transition =
                transitionService.transition(payment, PaymentStatus.FAILED, "PAYIN_INITIATE");
        payment.setProvider(finalRoute != null ? finalRoute.getProvider().getCode() : null);
        payment.setFailureReason(determineFailureReason(finalResult));
        payment.setRouteHealth("DEGRADED");
        paymentRepository.save(payment);
        if (transitionService.shouldNotifyWebhook(transition)) {
            webhookService.sendWebhook(payment);
        }
        return toResponse(payment);
    }

    private void saveRouteAttempt(Payment payment, PaymentProviderRoute route, PaymentResult result, boolean fallbackUsed) {
        LogEntry logEntry = new LogEntry();
        logEntry.setPaymentId(payment.getPaymentId());
        logEntry.setRouteUsed(routeName(route));
        logEntry.setProvider(route.getProvider().getCode());
        logEntry.setResponseTime(result == null ? 0.0 : result.getResponseTimeMs());
        logEntry.setStatus(result != null && result.isSuccess()
                ? LogStatus.SUCCESS
                : result != null && result.isProviderTransactionCreated() ? LogStatus.PENDING : LogStatus.FAILED);
        logEntry.setFailureReason(result != null && result.isSuccess() ? null : determineFailureReason(result));
        logEntry.setErrorType(result == null ? null : result.getErrorType());
        logEntry.setFallbackUsed(fallbackUsed);
        String message = result == null ? "No provider response" : result.getRawResponse();
        if (message != null && message.length() > 5000) {
            message = message.substring(0, 4990) + "...[TRUNCATED]";
        }
        logEntry.setMessage(message);
        logEntryRepository.save(logEntry);
    }

    private String routeName(PaymentProviderRoute route) {
        return route.getProvider().getCode() + "_" + route.getOperator() + "_" + route.getCountry();
    }

    private String defaultInitiationSource(String initiationSource) {
        return initiationSource == null || initiationSource.isBlank()
                ? "API"
                : initiationSource.trim().toUpperCase();
    }

    private PayInProviderRequest toProviderRequest(Payment payment, PaymentProviderRoute route,
            ProviderCredentials credentials, String environment) {
        return PayInProviderRequest.builder()
                .credentials(credentials)
                .environment(environment)
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount().longValue())
                .country(payment.getCountry())
                .operator(payment.getOperator())
                .providerChannel(route.getProviderChannel())
                .flowType(route.getFlowType())
                .phone(payment.getCustomerPhone())
                .firstname(payment.getCustomerFirstname())
                .lastname(payment.getCustomerLastname())
                .email(payment.getCustomerEmail())
                .returnUrl(payment.getReturnUrl())
                .cancelUrl(payment.getCancelUrl())
                .build();
    }

    private ProviderCredentials credentialsFor(Payment payment) {
        if (payment.getMerchantProviderAccountId() == null) {
            return null;
        }
        return providerAccountService.decrypt(providerAccountService.getAccount(payment.getMerchantProviderAccountId()));
    }

    private PayInProviderClient clientFor(String provider) {
        if ("KKIAPAY".equalsIgnoreCase(provider)) {
            return kkiapayClient;
        }
        if ("PAYDUNYA".equalsIgnoreCase(provider)) {
            return payDunyaClient;
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }

    private boolean shouldFallback(PaymentResult result) {
        if (result == null || result.isProviderTransactionCreated()) {
            return false;
        }
        return result.getErrorType() == ErrorType.NETWORK
                || result.getErrorType() == ErrorType.TIMEOUT
                || result.getErrorType() == ErrorType.PROVIDER_DOWN
                || result.getErrorType() == ErrorType.AUTHENTICATION
                || result.getErrorType() == ErrorType.INTERNAL_ERROR;
    }

    private String determineFailureReason(PaymentResult result) {
        if (result == null || result.getErrorType() == null) {
            return "GENERIC_FAILURE";
        }
        return switch (result.getErrorType()) {
            case AUTHENTICATION -> "AUTHENTICATION_FAILED";
            case BAD_REQUEST -> "BAD_REQUEST";
            case NETWORK -> "NETWORK_ERROR";
            case TIMEOUT -> "TIMEOUT";
            case PROVIDER_DOWN -> "PROVIDER_DOWN";
            case INTERNAL_ERROR -> "INTERNAL_ERROR";
            default -> "GENERIC_FAILURE";
        };
    }

    private void assertOwner(MerchantApiPrincipal principal, Payment payment) {
        if (payment.getApiKeyId() == null || !payment.getApiKeyId().equals(principal.getApiKey().getId())) {
            throw new IllegalArgumentException("Payment not found");
        }
    }

    private MerchantPaymentResponse toResponse(Payment payment) {
        MerchantPaymentResponse.NextAction nextAction = null;
        if (payment.getNextActionType() != null) {
            nextAction = MerchantPaymentResponse.NextAction.builder()
                    .type(payment.getNextActionType())
                    .provider(payment.getProvider())
                    .message("SUBMIT_OTP".equals(payment.getNextActionType())
                            ? "Submit the Orange Money CI OTP to continue this payment"
                            : "Additional action required")
                    .build();
        }
        return MerchantPaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getStatus())
                .paymentUrl(payment.getPaymentUrl())
                .provider(payment.getProvider())
                .flowType(payment.getFlowType())
                .operator(payment.getOperator())
                .country(payment.getCountry())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .nextAction(nextAction)
                .failureReason(payment.getFailureReason())
                .build();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeOperator(String value) {
        String normalized = normalize(value);
        if ("ORANGE".equals(normalized) || "ORANGE_MONEY".equals(normalized)) return "OM";
        if ("FREE".equals(normalized) || "FREEMONEY".equals(normalized) || "MIXX BY YAS".equals(normalized)) return "MIXX";
        if ("TMONEY".equals(normalized) || "T-MONEY".equals(normalized) || "MIXXBYYAS".equals(normalized)) return "MIXX";
        return normalized;
    }

    private boolean requiresPhone(String country, String operator) {
        return !"INT".equals(country)
                && !"VISA".equals(operator)
                && !"MASTERCARD".equals(operator);
    }
}
