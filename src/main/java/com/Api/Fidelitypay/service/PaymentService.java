package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LogEntryRepository logEntryRepository;
    private final RouteSelectionService routeSelectionService;
    private final WebhookService webhookService;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            LogEntryRepository logEntryRepository,
            RouteSelectionService routeSelectionService,
            WebhookService webhookService,
            KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient) {
        this.paymentRepository = paymentRepository;
        this.logEntryRepository = logEntryRepository;
        this.routeSelectionService = routeSelectionService;
        this.webhookService = webhookService;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
    }

    /**
     * Options disponibles par pays
     */
    public List<String> getOptionsByCountry(String country) {
        return List.of("Wave", "Moov", "Orange Money");
    }

    /**
     * Initie un paiement avec fallback réel entre plusieurs providers
     */
    public Payment initiatePayment(double amount, String country, String operatorInput, String phone, String firstname,
            String lastname, String email) {

        String operator = (operatorInput != null) ? operatorInput.toUpperCase().trim() : "UNKNOWN";
        String countryCode = (country != null) ? country.toUpperCase().trim() : "UNKNOWN";
        String paymentId = UUID.randomUUID().toString();

        // 1. Initialiser l'objet Payment
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOperator(operator);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCost(BigDecimal.ZERO);
        payment.setCountry(countryCode);
        payment.setUsedFallback(false);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setAttemptCount(0);

        paymentRepository.save(payment);

        // 2. Récupérer toutes les routes disponibles triées par score
        List<Route> availableRoutes = routeSelectionService.getSortedRoutes(operator);

        if (availableRoutes.isEmpty()) {
            log.error("No available route for operator {}", operator);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("NO_ROUTE_AVAILABLE");
            paymentRepository.save(payment);
            return payment;
        }

        PaymentResult finalResult = null;
        Route finalRouteUsed = null;
        boolean success = false;
        int attempt = 0;
        String primaryProvider = availableRoutes.get(0).getProvider();

        // 3. Boucle de fallback (essaye chaque route jusqu'au succès ou erreur
        // non-technique)
        for (Route route : availableRoutes) {
            attempt++;
            log.info("🚀 Attempt {}: Trying route {} (Provider: {}) for payment {}",
                    attempt, route.getName(), route.getProvider(), paymentId);

            PaymentResult result = executeRoute(route, amount, countryCode, operator, phone, firstname, lastname,
                    email);

            if (result != null && result.isSuccess()) {
                success = true;
                finalResult = result;
                finalRouteUsed = route;
                payment.setUsedFallback(attempt > 1);
                payment.setAttemptCount(attempt);
                if (attempt > 1) {
                    payment.setFallbackReason("PRIMARY_ROUTE_FAILED");
                    log.info("✅ Fallback SUCCESS on attempt {} with producer {}", attempt, route.getProvider());
                } else {
                    log.info("✅ Primary route SUCCESS with producer {}", route.getProvider());
                }
                break;
            }

            // Échec de la tentative
            String errorMsg = (result != null) ? result.getRawResponse() : "NO_RESPONSE";
            ErrorType errorType = (result != null) ? result.getErrorType() : ErrorType.UNKNOWN;

            // Log de l'échec pour cette route
            logRouteAttempt(attempt == 1 ? "PRIMARY" : "FALLBACK", route, false, errorMsg, errorType);
            saveLog(paymentId, route, result, false, determineFailureReason(errorMsg, errorType),
                    errorType, attempt > 1, (attempt > 1 ? "MULTI_STEP_FALLBACK" : null), primaryProvider);

            // Est-ce une erreur technique permettant de continuer ?
            if (!shouldTriggerFallback(errorMsg, errorType)) {
                log.warn("❌ Stop fallback: Non-technical error encountered: {}", errorMsg);
                finalResult = result;
                finalRouteUsed = route;
                break;
            }

            // Si c'était la dernière route, on s'arrête
            finalResult = result;
            finalRouteUsed = route;
            if (attempt >= availableRoutes.size()) {
                log.error("❌ All available routes failed for operator {}", operator);
            } else {
                log.info("🔄 Technical error, trying next available route...");
            }
        }

        // 4. Finaliser le paiement
        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        if (finalRouteUsed != null) {
            payment.setRouteName(finalRouteUsed.getName());
            payment.setProvider(finalRouteUsed.getProvider());
            payment.setCost(BigDecimal.valueOf(success ? finalRouteUsed.getCost() : 0.0));

            String health = success ? "HEALTHY"
                    : (isTechnicalError((finalResult != null ? finalResult.getRawResponse() : null)) ? "DOWN"
                            : "DEGRADED");
            payment.setRouteHealth(health);
        }

        if (finalResult != null) {
            payment.setProviderPaymentId(finalResult.getProviderId());
            payment.setProviderResponse(finalResult.getRawResponse());
            payment.setPaymentUrl(finalResult.getPaymentUrl());
            payment.setProviderResponseTimeMs((long) finalResult.getResponseTimeMs());
            payment.setErrorType(finalResult.getErrorType());

            if (!success) {
                payment.setFailureReason(
                        determineFailureReason(finalResult.getRawResponse(), finalResult.getErrorType()));
            }
        }

        paymentRepository.save(payment);

        // Log final (seulement si pas déjà loggé dans la boucle pour l'échec final ou
        // le succès)
        saveLog(paymentId, finalRouteUsed, finalResult, success, payment.getFailureReason(),
                payment.getErrorType(), payment.isUsedFallback(), payment.getFallbackReason(), primaryProvider);

        if (success) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private PaymentResult executeRoute(Route route, double amount, String country, String operator, String phone,
            String firstname, String lastname, String email) {
        if (route == null)
            return null;

        try {
            switch (route.getProvider().toUpperCase()) {
                case "KKIAPAY":
                    return kkiapayClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
                case "PAYDUNYA":
                    return payDunyaClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
                default:
                    log.warn("Unsupported provider {}", route.getProvider());
                    return null;
            }
        } catch (Exception e) {
            log.error("Exception during route execution: {}", route.getName(), e);
            PaymentResult error = new PaymentResult(false);
            error.setRawResponse("EXCEPTION: " + e.getMessage());
            error.setErrorType(ErrorType.INTERNAL_ERROR);
            return error;
        }
    }

    private void saveLog(String paymentId, Route routeUsed, PaymentResult result, boolean success,
            String failureReason, ErrorType errorType, boolean fallbackUsed, String fallbackReason,
            String primaryProvider) {
        try {
            LogEntry logEntry = new LogEntry();
            logEntry.setPaymentId(paymentId);
            logEntry.setRouteUsed(routeUsed != null ? routeUsed.getName() : "UNKNOWN");
            logEntry.setProvider(routeUsed != null ? routeUsed.getProvider() : "UNKNOWN");
            logEntry.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
            logEntry.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
            logEntry.setFailureReason(failureReason);
            logEntry.setErrorType(errorType);
            logEntry.setFallbackUsed(fallbackUsed);

            StringBuilder messageBuilder = new StringBuilder();
            if (fallbackUsed) {
                messageBuilder.append("Fallback active (Primary was: ").append(primaryProvider).append("). ");
            }
            if (result != null && result.getRawResponse() != null) {
                messageBuilder.append("Provider response: ").append(result.getRawResponse());
            }

            String message = messageBuilder.toString();
            if (message.length() > 5000) {
                message = message.substring(0, 4990) + "...[TRUNCATED]";
            }
            logEntry.setMessage(message);

            logEntryRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save log for payment {}", paymentId, e);
        }
    }

    private String determineFailureReason(String errorMessage, ErrorType errorType) {
        if (errorMessage == null)
            return "UNKNOWN_ERROR";

        if (errorType != null) {
            switch (errorType) {
                case AUTHENTICATION:
                    return "AUTHENTICATION_FAILED";
                case TIMEOUT:
                    return "TIMEOUT";
                case NETWORK:
                    return "NETWORK_ERROR";
                case PROVIDER_DOWN:
                    return "PROVIDER_DOWN";
                case BAD_REQUEST:
                    return "BAD_REQUEST";
                case INTERNAL_ERROR:
                    return "INTERNAL_ERROR";
            }
        }

        String error = errorMessage.toUpperCase();
        if (error.contains("SOLDE") || error.contains("INSUFFICIENT") || error.contains("FUNDS"))
            return "INSUFFICIENT_FUNDS";
        if (error.contains("TIMEOUT") || error.contains("TIME_OUT"))
            return "TIMEOUT";
        if (error.contains("PHONE") || error.contains("NUMBER") || error.contains("INVALID"))
            return "INVALID_PHONE_NUMBER";
        if (error.contains("AUTH") || error.contains("TOKEN") || error.contains("401"))
            return "AUTHENTICATION_FAILED";
        if (error.contains("CANCEL"))
            return "CANCELLED_BY_USER";

        return "GENERIC_FAILURE";
    }

    private boolean shouldTriggerFallback(String errorMessage, ErrorType errorType) {
        if (errorType != null) {
            return errorType == ErrorType.AUTHENTICATION ||
                    errorType == ErrorType.TIMEOUT ||
                    errorType == ErrorType.NETWORK ||
                    errorType == ErrorType.PROVIDER_DOWN ||
                    errorType == ErrorType.INTERNAL_ERROR;
        }
        return isTechnicalError(errorMessage);
    }

    private boolean isTechnicalError(String errorMessage) {
        if (errorMessage == null)
            return false;
        String error = errorMessage.toUpperCase();
        return error.contains("TIMEOUT") || error.contains("CONNECTION") || error.contains("500") ||
                error.contains("NETWORK") || error.contains("503") || error.contains("UNAVAILABLE");
    }

    private void logRouteAttempt(String type, Route route, boolean success, String errorMsg, ErrorType errorType) {
        if (success) {
            log.info("✅ {} route SUCCESS: {}", type, route.getName());
        } else {
            log.warn("❌ {} route FAILED: {} | Type: {} | Error: {}",
                    type, route.getName(), errorType, errorMsg);
        }
    }

    public Payment getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId).orElse(null);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<String> getAllPaymentCountries() {
        return paymentRepository.findDistinctCountries();
    }

    public Map<String, Boolean> checkProvidersHealth() {
        Map<String, Boolean> health = new HashMap<>();
        health.put("KKIAPAY", kkiapayClient.isAvailable());
        health.put("PAYDUNYA", payDunyaClient.isAvailable());
        return health;
    }

    public void processKkiapayCallback(com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback) {
        log.info("Processing Kkiapay callback for transaction: {}", callback.getTransactionId());
        paymentRepository.findByProviderPaymentId(callback.getTransactionId()).ifPresent(payment -> {
            boolean wasPending = payment.getStatus() == PaymentStatus.PENDING;
            payment.setStatus(callback.isPaymentSucces() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            if (wasPending && payment.getStatus() == PaymentStatus.SUCCESS) {
                webhookService.sendWebhook(payment);
            }
        });
    }
}