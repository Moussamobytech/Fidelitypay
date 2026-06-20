package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.enums.ErrorType;
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
    private final PaymentStateTransitionService transitionService;

    public PaymentService(
            PaymentRepository paymentRepository,
            LogEntryRepository logEntryRepository,
            WebhookService webhookService,
            KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient,
            PaymentRouteService routeService,
            PaymentStateTransitionService transitionService) {
        this.paymentRepository = paymentRepository;
        this.logEntryRepository = logEntryRepository;
        this.webhookService = webhookService;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
        this.routeService = routeService;
        this.transitionService = transitionService;
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
            transitionService.transition(payment, PaymentStatus.FAILED, "PAYMENT_INITIATE");
            payment.setFailureReason("INVALID_PHONE_NUMBER");
            paymentRepository.save(payment);
            log.warn("Invalid phone number {} for country {}", phone, countryCode);
            return payment;
        }

        // 2. Use the same scored provider-route catalog as the merchant API flow.
        List<PaymentProviderRoute> routesToTry = routeService.findAvailablePayIn(countryCode, operator, "LIVE",
                user == null ? null : user.getId());

        if (routesToTry.isEmpty()) {
            transitionService.transition(payment, PaymentStatus.FAILED, "PAYMENT_INITIATE");
            payment.setFailureReason("NO_PROVIDER_AVAILABLE_FOR_COUNTRY");
            paymentRepository.save(payment);
            log.warn("No providers configured in dashboard for country {} and operator {}", countryCode, operator);
            return payment;
        }

        PaymentResult finalResult = null;
        PaymentProviderRoute finalRoute = null;
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

            PaymentResult result = executeProvider(route, payment, phone, firstname, lastname, email);

            if (result != null && result.isSuccess()) {
                success = true;
                finalResult = result;
                finalRoute = route;
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
            saveLog(paymentId, route, result, false, determineFailureReason(errorMsg, errorType),
                    errorType, attempt > 1, (attempt > 1 ? "MULTI_STEP_FALLBACK" : null), primaryProvider);

            // Est-ce une erreur technique permettant de continuer ?
            if (!shouldTriggerFallback(errorMsg, errorType)) {
                log.warn("❌ Stop fallback: Non-technical error encountered: {}", errorMsg);
                finalResult = result;
                finalRoute = route;
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
            finalRoute = route;
        }

        // 4. Finaliser l'initialisation.
        // Un succès fournisseur signifie seulement que le checkout/transaction provider
        // a été créé. Le paiement reste PENDING jusqu'au callback fournisseur final.
        PaymentStateTransitionService.TransitionResult transition =
                transitionService.transition(payment, success ? PaymentStatus.PENDING : PaymentStatus.FAILED, "PAYMENT_INITIATE");
        
        // ✅ FIX: Set attemptCount for all outcomes (was only set on success)
        payment.setAttemptCount(attempt);
        
        // ✅ FIX: Set fallbackReason when fallback was needed but unavailable
        if (!success && fallbackNeededButUnavailable && payment.getFallbackReason() == null) {
            payment.setFallbackReason("NO_FALLBACK_PROVIDER_AVAILABLE");
        }

        if (finalRoute != null) {
            payment.setRouteName(routeName(finalRoute));
            payment.setProvider(providerCode(finalRoute));
            payment.setCost(BigDecimal.valueOf(finalRoute.getCost()));
            if (finalRoute.getFlowType() != null) {
                payment.setFlowType(finalRoute.getFlowType().name());
            }
            payment.setProviderChannel(finalRoute.getProviderChannel());

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
            payment.setErrorType(finalResult.getErrorType());

            if (success) {
                payment.setFailureReason(null);
            } else {
                payment.setFailureReason(
                        determineFailureReason(finalResult.getRawResponse(), finalResult.getErrorType()));
            }
        }

        paymentRepository.save(payment);

        saveLog(paymentId, finalRoute, finalResult, success, payment.getFailureReason(),
                payment.getErrorType(), payment.isUsedFallback(), payment.getFallbackReason(), primaryProvider);

        if (transitionService.shouldNotifyWebhook(transition)) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private boolean isPhoneNumberValidForCountry(String phone, String country) {
        if (phone == null || phone.isEmpty() || country == null || country.isEmpty()) {
            return true;
        }
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (country.equalsIgnoreCase("MALI") || country.equalsIgnoreCase("ML")) {
            return isValidRegionalPhone(cleanPhone, "223", 8, 9);
        } else if (country.equalsIgnoreCase("BENIN") || country.equalsIgnoreCase("BJ")) {
            return isValidRegionalPhone(cleanPhone, "229", 8, 9);
        } else if (country.equalsIgnoreCase("SENEGAL") || country.equalsIgnoreCase("SN")) {
            return isValidRegionalPhone(cleanPhone, "221", 9, 10);
        } else if (country.equalsIgnoreCase("COTE D'IVOIRE") || country.equalsIgnoreCase("CI")) {
            return isValidRegionalPhone(cleanPhone, "225", 10, 11);
        } else if (country.equalsIgnoreCase("TOGO") || country.equalsIgnoreCase("TG")) {
            return isValidRegionalPhone(cleanPhone, "228", 8, 9);
        } else if (country.equalsIgnoreCase("GUINEA") || country.equalsIgnoreCase("GN")) {
            return isValidRegionalPhone(cleanPhone, "224", 8, 9);
        }
        return true;
    }

    private boolean isValidRegionalPhone(String digits, String countryPrefix, int minLocalLength, int maxLocalLength) {
        if (digits == null || digits.isBlank()) {
            return false;
        }
        String local = digits;
        if (digits.startsWith("00" + countryPrefix)) {
            local = digits.substring(countryPrefix.length() + 2);
        } else if (digits.startsWith(countryPrefix)) {
            local = digits.substring(countryPrefix.length());
        } else if (digits.startsWith("0")) {
            local = digits.substring(1);
        }
        return local.length() >= minLocalLength && local.length() <= maxLocalLength;
    }

    private PaymentResult executeProvider(PaymentProviderRoute route, Payment payment, String phone,
            String firstname, String lastname, String email) {
        String providerName = providerCode(route);
        if (providerName == null)
            return null;

        PayInProviderRequest request = PayInProviderRequest.builder()
                .environment("LIVE")
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount().longValue())
                .country(payment.getCountry())
                .operator(payment.getOperator())
                .providerChannel(route.getProviderChannel())
                .flowType(route.getFlowType())
                .phone(phone)
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .build();

        try {
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

    private String routeName(PaymentProviderRoute route) {
        if (route == null) {
            return "UNKNOWN";
        }
        String provider = providerCode(route);
        return (provider != null ? provider : "UNKNOWN") + "_" + route.getOperator() + "_" + route.getCountry();
    }

    private void saveLog(String paymentId, PaymentProviderRoute route, PaymentResult result, boolean success,
            String failureReason, ErrorType errorType, boolean fallbackUsed, String fallbackReason,
            String primaryProvider) {
        saveLog(paymentId, providerCode(route), routeName(route), result, success,
                failureReason, errorType, fallbackUsed, fallbackReason, primaryProvider);
    }

    private void saveLog(String paymentId, String providerUsed, String routeUsed, PaymentResult result, boolean success,
            String failureReason, ErrorType errorType, boolean fallbackUsed, String fallbackReason,
            String primaryProvider) {
        try {
            LogEntry logEntry = new LogEntry();
            logEntry.setPaymentId(paymentId);
            logEntry.setRouteUsed(routeUsed != null ? routeUsed : "UNKNOWN");
            logEntry.setProvider(providerUsed != null ? providerUsed : "UNKNOWN");
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
                case UNKNOWN:
                default:
                    return "GENERIC_FAILURE";
            }
        }

        String error = errorMessage.toUpperCase();
        if (error.contains("SOLDE") || error.contains("INSUFFICIENT") || error.contains("FUNDS"))
            return "INSUFFICIENT_FUNDS";
        if (error.contains("TIMEOUT") || error.contains("TIME_OUT"))
            return "TIMEOUT";
        if (error.contains("PHONE") || error.contains("NUMBER") || error.contains("INVALID PHONE"))
            return "INVALID_PHONE_NUMBER";
        if (error.contains("PAYMENT CHANNEL") || error.contains("INVALID_OPERATOR") || error.contains("NOT A VALID PAYMENT CHANNEL"))
            return "INVALID_OPERATOR";
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
