package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.FailureReason;
import com.Api.Fidelitypay.enums.FailureStage;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.service.failure.PaymentFailure;
import com.Api.Fidelitypay.service.failure.PaymentFailureClassifier;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.PayInProviderRequest;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LogEntryRepository logEntryRepository;
    private final WebhookService webhookService;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;
    private final PaymentRouteService routeService;
    private final PaymentFailureClassifier failureClassifier;

    public PaymentService(
            PaymentRepository paymentRepository,
            LogEntryRepository logEntryRepository,
            WebhookService webhookService,
            KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient,
            PaymentRouteService routeService,
            PaymentFailureClassifier failureClassifier) {
        this.paymentRepository = paymentRepository;
        this.logEntryRepository = logEntryRepository;
        this.webhookService = webhookService;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
        this.routeService = routeService;
        this.failureClassifier = failureClassifier;
    }

    /**
     * Retourne les opérateurs disponibles pour un pays spécifique
     */
    public List<String> getOptionsByCountry(String country) {
        if (country == null || country.isEmpty()) return List.of();
        return routeService.findAvailablePayInOperators(country);
    }

    /**
     * Initie un paiement through the scored provider-route catalog.
     */
    public Payment initiatePayment(User user, double amount, String country, String operatorInput, String phone,
            String firstname,
            String lastname, String email) {

        String operator = normalizeOperator(operatorInput);
        String countryCode = (country != null) ? country.toUpperCase().trim() : "UNKNOWN";
        String paymentId = UUID.randomUUID().toString();

        // 1. Initialiser l'objet Payment
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setUser(user);
        payment.setOperator(operator);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCost(BigDecimal.ZERO);
        payment.setCountry(countryCode);
        payment.setUsedFallback(false);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setAttemptCount(0);

        paymentRepository.save(payment);

        // Validation du numéro de téléphone par rapport au pays
        if (!isPhoneNumberValidForCountry(phone, countryCode)) {
            payment.setStatus(PaymentStatus.FAILED);
            failureClassifier.apply(payment,
                    failureClassifier.known(FailureReason.INVALID_PHONE_NUMBER, FailureStage.VALIDATION));
            paymentRepository.save(payment);
            log.warn("Invalid phone number {} for country {}", phone, countryCode);
            return payment;
        }

        // 2. Use the same scored provider-route catalog as the merchant API flow.
        List<PaymentProviderRoute> routesToTry = routeService.findAvailablePayIn(countryCode, operator, "LIVE",
                user == null ? null : user.getId());
        List<String> providersToTry = routesToTry.stream()
                .map(this::providerCode)
                .distinct()
                .toList();

        log.warn(":::::::::::::::: THE PROVIDERS = {} ", providersToTry);


        if (providersToTry.isEmpty()) {
            payment.setStatus(PaymentStatus.FAILED);
            failureClassifier.apply(payment, failureClassifier.known(
                    FailureReason.NO_PROVIDER_AVAILABLE_FOR_COUNTRY, FailureStage.ROUTING));
            paymentRepository.save(payment);
            log.warn("No providers configured in dashboard for country {} and operator {}", countryCode, operator);
            return payment;
        }

        PaymentResult finalResult = null;
        String finalProviderUsed = null;
        PaymentProviderRoute finalRouteUsed = null;
        boolean success = false;
        int attempt = 0;
        String primaryProvider = providerCode(routesToTry.get(0));
        boolean fallbackNeededButUnavailable = false;  // ✅ FIX: Track if fallback was needed but no providers left

        // 3. Boucle de fallback (essaye chaque provider)
        for (PaymentProviderRoute route : routesToTry) {
            attempt++;
            String providerName = providerCode(route);
            log.info("🚀 Attempt {}: Trying provider {} for payment {}",
                    attempt, providerName, paymentId);

            PaymentResult result = executeProvider(route, paymentId, amount, countryCode, operator, phone, firstname, lastname, email);

            if (result != null && result.isSuccess()) {
                success = true;
                finalResult = result;
                finalProviderUsed = providerName;
                finalRouteUsed = route;
                payment.setUsedFallback(attempt > 1);
                payment.setAttemptCount(attempt);
                if (attempt > 1) {
                    payment.setFallbackReason("PRIMARY_PROVIDER_FAILED");
                    log.info("✅ Fallback SUCCESS on attempt {} with provider {}", attempt, providerName);
                } else {
                    log.info("✅ Primary provider SUCCESS with {}", providerName);
                }
                break;
            }

            // Échec de la tentative
            String errorMsg = (result != null) ? result.getRawResponse() : "NO_RESPONSE";
            ErrorType errorType = (result != null) ? result.getErrorType() : ErrorType.UNKNOWN;

            // Log de l'échec
            logProviderAttempt(attempt == 1 ? "PRIMARY" : "FALLBACK", providerName, false, errorMsg, errorType);
            PaymentFailure attemptFailure = failureClassifier.classifyProviderResult(result, FailureStage.PROVIDER_INIT);
            saveLog(paymentId, providerName, result, false, attemptFailure, attempt > 1,
                    (attempt > 1 ? "MULTI_STEP_FALLBACK" : null), primaryProvider);

            // Est-ce une erreur technique permettant de continuer ?
            if (!shouldTriggerFallback(errorMsg, errorType)) {
                log.warn("❌ Stop fallback: Non-technical error encountered: {}", errorMsg);
                finalResult = result;
                finalProviderUsed = providerName;
                finalRouteUsed = route;
                break;
            }

            // ✅ FIX: Mark if fallback was needed but no more providers available
            if (attempt >= routesToTry.size()) {
                log.error("❌ All providers failed for operator {}. No fallback available.", operator);
                fallbackNeededButUnavailable = true;
            } else {
                log.info("🔄 Technical error detected, trying next provider...");
            }

            finalResult = result;
            finalProviderUsed = providerName;
            finalRouteUsed = route;
        }

        // 4. Finaliser l'initialisation.
        // Un succès fournisseur signifie seulement que le checkout/transaction provider
        // a été créé. Le paiement reste PENDING jusqu'au callback fournisseur final.
        payment.setStatus(success ? PaymentStatus.PENDING : PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        
        // ✅ FIX: Set attemptCount for all outcomes (was only set on success)
        payment.setAttemptCount(attempt);
        
        // ✅ FIX: Set fallbackReason when fallback was needed but unavailable
        if (!success && fallbackNeededButUnavailable && payment.getFallbackReason() == null) {
            payment.setFallbackReason("NO_FALLBACK_PROVIDER_AVAILABLE");
        }

        if (finalProviderUsed != null) {
            payment.setRouteName(finalRouteUsed != null ? routeName(finalRouteUsed) : finalProviderUsed);
            payment.setProvider(finalProviderUsed);
            if (finalRouteUsed != null) {
                payment.setProviderChannel(finalRouteUsed.getProviderChannel());
                payment.setFlowType(finalRouteUsed.getFlowType() != null ? finalRouteUsed.getFlowType().name() : null);
            }
            payment.setCost(BigDecimal.ZERO);

            String health = success ? "HEALTHY"
                    : (isTechnicalError((finalResult != null ? finalResult.getRawResponse() : null)) ? "DOWN"
                            : "DEGRADED");
            payment.setRouteHealth(health);
        }

        if (finalResult != null) {
            payment.setProviderPaymentId(finalResult.getProviderId());
            payment.setProviderResponse(finalResult.getRawResponse());
            payment.setPaymentUrl(finalResult.getPaymentUrl());
            
            if (finalResult.getActualOperator() != null) {
                payment.setOperator(finalResult.getActualOperator().toUpperCase());
            }
            
            payment.setProviderResponseTimeMs((long) finalResult.getResponseTimeMs());

            if (success) {
                failureClassifier.clear(payment);
            } else {
                failureClassifier.apply(payment,
                        failureClassifier.classifyProviderResult(finalResult, FailureStage.PROVIDER_INIT));
            }
        }

        paymentRepository.save(payment);

        PaymentFailure finalFailure = success ? null
                : failureClassifier.classifyProviderResult(finalResult, FailureStage.PROVIDER_INIT);
        saveLog(paymentId, finalProviderUsed, finalResult, success, finalFailure, payment.isUsedFallback(),
                payment.getFallbackReason(), primaryProvider);

        if (payment.getStatus() == PaymentStatus.FAILED) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private boolean isPhoneNumberValidForCountry(String phone, String country) {
        if (phone == null || phone.isEmpty() || country == null || country.isEmpty()) {
            return true;
        }
        String cleanPhone = phone.replaceAll("\\s+", "");
        if (country.equalsIgnoreCase("MALI") || country.equalsIgnoreCase("ML")) {
            return cleanPhone.startsWith("+223") || cleanPhone.startsWith("00223") || (!cleanPhone.startsWith("+") && cleanPhone.length() >= 8 && cleanPhone.length() <= 9);
        } else if (country.equalsIgnoreCase("BENIN") || country.equalsIgnoreCase("BJ")) {
            return cleanPhone.startsWith("+229") || cleanPhone.startsWith("00229") || (!cleanPhone.startsWith("+") && cleanPhone.length() >= 8 && cleanPhone.length() <= 9);
        } else if (country.equalsIgnoreCase("SENEGAL") || country.equalsIgnoreCase("SN")) {
            return cleanPhone.startsWith("+221") || cleanPhone.startsWith("00221") || (!cleanPhone.startsWith("+") && cleanPhone.length() >= 9 && cleanPhone.length() <= 10);
        } else if (country.equalsIgnoreCase("COTE D'IVOIRE") || country.equalsIgnoreCase("CI")) {
            return cleanPhone.startsWith("+225") || cleanPhone.startsWith("00225") || (!cleanPhone.startsWith("+") && cleanPhone.length() >= 10 && cleanPhone.length() <= 11);
        } else if (country.equalsIgnoreCase("TOGO") || country.equalsIgnoreCase("TG")) {
            return cleanPhone.startsWith("+228") || cleanPhone.startsWith("00228") || (!cleanPhone.startsWith("+") && cleanPhone.length() >= 8 && cleanPhone.length() <= 9);
        } else if (country.equalsIgnoreCase("GUINEA") || country.equalsIgnoreCase("GN")) {
            return cleanPhone.startsWith("+224") || cleanPhone.startsWith("00224") || (!cleanPhone.startsWith("+") && cleanPhone.length() >= 8 && cleanPhone.length() <= 9);
        }
        return true;
    }

    private PaymentResult executeProvider(PaymentProviderRoute route, String paymentId, double amount, String country,
            String operator, String phone,
            String firstname, String lastname, String email) {
        String providerName = providerCode(route);
        if (providerName == null)
            return null;

        try {
            PayInProviderRequest request = PayInProviderRequest.builder()
                    .paymentId(paymentId)
                    .amount((long) amount)
                    .country(country)
                    .operator(operator)
                    .providerChannel(route.getProviderChannel())
                    .flowType(resolveFlowType(providerName, route.getFlowType(), operator))
                    .phone(phone)
                    .firstname(firstname)
                    .lastname(lastname)
                    .email(email)
                    .build();
            switch (providerName.toUpperCase()) {
                case "KKIAPAY":
                    return kkiapayClient.initiatePayIn(request);
                case "PAYDUNYA":
                    return payDunyaClient.initiatePayIn(request);
                default:
                    log.warn("Unsupported provider {}", providerName);
                    return null;
            }
        } catch (Exception e) {
            log.error("Exception during provider execution: {}", providerName, e);
            PaymentResult error = new PaymentResult(false);
            error.setRawResponse("EXCEPTION: " + e.getMessage());
            error.setErrorType(ErrorType.INTERNAL_ERROR);
            return error;
        }
    }

    private String providerCode(PaymentProviderRoute route) {
        return route != null && route.getProvider() != null ? route.getProvider().getCode() : null;
    }

    private PaymentFlowType resolveFlowType(String providerName, PaymentFlowType routeFlowType, String operator) {
        if (routeFlowType != null) {
            return routeFlowType;
        }
        if ("PAYDUNYA".equalsIgnoreCase(providerName)) {
            return PaymentFlowType.HOSTED_CHECKOUT;
        }
        return "WAVE".equalsIgnoreCase(operator)
                ? PaymentFlowType.WAVE_REDIRECT
                : PaymentFlowType.MOBILE_MONEY_REQUEST;
    }

    private String routeName(PaymentProviderRoute route) {
        return providerCode(route) + "_" + route.getOperator() + "_" + route.getCountry();
    }

    private void saveLog(String paymentId, String providerUsed, PaymentResult result, boolean success,
            PaymentFailure failure, boolean fallbackUsed, String fallbackReason, String primaryProvider) {
        try {
            LogEntry logEntry = new LogEntry();
            logEntry.setPaymentId(paymentId);
            logEntry.setRouteUsed(providerUsed != null ? providerUsed : "UNKNOWN");
            logEntry.setProvider(providerUsed != null ? providerUsed : "UNKNOWN");
            logEntry.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
            logEntry.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
            if (failure != null) {
                logEntry.setFailureReason(failure.reasonCode());
                logEntry.setErrorType(failure.errorType());
            }
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

    private boolean shouldTriggerFallback(String errorMessage, ErrorType errorType) {
        if (errorType != null) {
            return errorType == ErrorType.AUTHENTICATION ||
                    errorType == ErrorType.TIMEOUT ||
                    errorType == ErrorType.NETWORK ||
                    errorType == ErrorType.PROVIDER_DOWN ||
                    errorType == ErrorType.INTERNAL_ERROR ||
                    errorType == ErrorType.BAD_REQUEST; // For 404/400 errors that might be transient or route-specific
        }
        return isTechnicalError(errorMessage);
    }

    private boolean isTechnicalError(String errorMessage) {
        if (errorMessage == null)
            return false;
        String error = errorMessage.toUpperCase();
        return error.contains("TIMEOUT") || error.contains("CONNECTION") || error.contains("500") ||
                error.contains("NETWORK") || error.contains("503") || error.contains("UNAVAILABLE") ||
                error.contains("404") || error.contains("401") || error.contains("403");
    }

    private void logProviderAttempt(String type, String providerName, boolean success, String errorMsg, ErrorType errorType) {
        if (success) {
            log.info("✅ {} provider SUCCESS: {}", type, providerName);
        } else {
            log.warn("❌ {} provider FAILED: {} | Type: {} | Error: {}",
                    type, providerName, errorType, errorMsg);
        }
    }

    public Payment getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId).orElse(null);
    }

    public List<Payment> getAllPayments(User user) {
        if (user == null)
            return List.of();

        if (user.getRole() == User.Role.ADMIN) {
            return paymentRepository.findAllByOrderByCreatedAtDesc();
        }
        return paymentRepository.findByUserId(user.getId());
    }

    /**
     * ✅ Récupère les paiements pour un utilisateur spécifique (pour ADMIN)
     */
    public List<Payment> getPaymentsByUserId(String userId) {
        if (userId == null || userId.isEmpty())
            return List.of();
        return paymentRepository.findByUserId(userId);
    }

    public List<String> getAllPaymentCountries() {
        return paymentRepository.findDistinctCountries();
    }

    public List<String> getPaymentCountriesByUser(User user) {
        if (user == null)
            return List.of();
        return paymentRepository.findDistinctCountriesByUserId(user.getId());
    }

    public Map<String, Boolean> checkProvidersHealth() {
        Map<String, Boolean> health = new HashMap<>();
        health.put("KKIAPAY", kkiapayClient.isAvailable());
        health.put("PAYDUNYA", payDunyaClient.isAvailable());
        return health;
    }

    public void processKkiapayCallback(com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback) {
        log.info("Processing Kkiapay callback for transaction: {}", callback.getTransactionId());
        updatePaymentStatusFromProvider(callback.getTransactionId(), callback.isPaymentSucces());
    }

    public void processPayDunyaCallback(String token, boolean success) {
        log.info("Processing PayDunya callback for token: {}", token);
        updatePaymentStatusFromProvider(token, success);
    }

    private void updatePaymentStatusFromProvider(String providerId, boolean success) {
        paymentRepository.findByProviderPaymentId(providerId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("Ignoring callback for providerId={} because payment {} is already {}",
                        providerId, payment.getPaymentId(), payment.getStatus());
                return;
            }

            payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            if (success) {
                failureClassifier.clear(payment);
            } else {
                failureClassifier.apply(payment, failureClassifier.classifyCallback(PaymentStatus.FAILED));
            }
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            webhookService.sendWebhook(payment);
        });
    }
    private String normalizeOperator(String op) {
        if (op == null || op.isBlank()) {
            return "UNKNOWN";
        }
        String upper = op.toUpperCase().trim();
        if (upper.contains("MOOV")) return "MOOV";
        if (upper.contains("MTN")) return "MTN";
        if (upper.contains("WAVE")) return "WAVE";
        if (upper.contains("ORANGE") || upper.equals("OM")) return "ORANGE";
        if (upper.contains("FREE")) return "FREE";
        if (upper.contains("YAS") || upper.contains("MIXX")) return "YAS";
        if (upper.contains("TMO")) return "TMO";
        return upper;
    }
}
