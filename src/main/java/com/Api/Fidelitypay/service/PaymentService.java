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
    private final RouteSelectionService routeSelectionService;

    public PaymentService(
            PaymentRepository paymentRepository,
            LogEntryRepository logEntryRepository,
            WebhookService webhookService,
            KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient,
            RouteSelectionService routeSelectionService) {
        this.paymentRepository = paymentRepository;
        this.logEntryRepository = logEntryRepository;
        this.webhookService = webhookService;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
        this.routeSelectionService = routeSelectionService;
    }

    /**
     * Retourne les opérateurs disponibles pour un pays spécifique
     */
    public List<String> getOptionsByCountry(String country) {
        if (country == null || country.isEmpty()) return List.of();
        return routeSelectionService.getAvailableOperatorsByCountry(country);
    }

    /**
     * Initie un paiement avec fallback direct sur les agrégateurs (sans passer par la BDD des routes)
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
            payment.setFailureReason("INVALID_PHONE_NUMBER");
            paymentRepository.save(payment);
            log.warn("Invalid phone number {} for country {}", phone, countryCode);
            return payment;
        }

        // 2. Récupérer les providers ordonnés pour ce pays/opérateur
        List<String> providersToTry = routeSelectionService.getSortedRoutes(operator, countryCode)
                .stream()
                .map(Route::getProvider)
                .toList();

        if (providersToTry.isEmpty()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("NO_PROVIDER_AVAILABLE_FOR_COUNTRY");
            paymentRepository.save(payment);
            log.warn("No providers configured in dashboard for country {} and operator {}", countryCode, operator);
            return payment;
        }

        PaymentResult finalResult = null;
        String finalProviderUsed = null;
        boolean success = false;
        int attempt = 0;
        String primaryProvider = providersToTry.get(0);

        // 3. Boucle de fallback (essaye chaque provider)
        for (String providerName : providersToTry) {
            attempt++;
            log.info("🚀 Attempt {}: Trying provider {} for payment {}",
                    attempt, providerName, paymentId);

            PaymentResult result = executeProvider(providerName, amount, countryCode, operator, phone, firstname, lastname, email);

            if (result != null && result.isSuccess()) {
                success = true;
                finalResult = result;
                finalProviderUsed = providerName;
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
            saveLog(paymentId, providerName, result, false, determineFailureReason(errorMsg, errorType),
                    errorType, attempt > 1, (attempt > 1 ? "MULTI_STEP_FALLBACK" : null), primaryProvider);

            // Est-ce une erreur technique permettant de continuer ?
            if (!shouldTriggerFallback(errorMsg, errorType)) {
                log.warn("❌ Stop fallback: Non-technical error encountered: {}", errorMsg);
                finalResult = result;
                finalProviderUsed = providerName;
                break;
            }

            finalResult = result;
            finalProviderUsed = providerName;
            if (attempt >= providersToTry.size()) {
                log.error("❌ All providers failed for operator {}", operator);
            } else {
                log.info("🔄 Technical error, trying next provider...");
            }
        }

        // 4. Finaliser le paiement
        if (success) {
            payment.setStatus((finalResult != null && finalResult.isPending()) ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        payment.setUpdatedAt(LocalDateTime.now());

        if (finalProviderUsed != null) {
            payment.setRouteName(finalProviderUsed);
            payment.setProvider(finalProviderUsed);
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
            payment.setErrorType(finalResult.getErrorType());

            if (!success) {
                payment.setFailureReason(
                        determineFailureReason(finalResult.getRawResponse(), finalResult.getErrorType()));
            }
        }

        paymentRepository.save(payment);

        saveLog(paymentId, finalProviderUsed, finalResult, success, payment.getFailureReason(),
                payment.getErrorType(), payment.isUsedFallback(), payment.getFallbackReason(), primaryProvider);

        webhookService.sendWebhook(payment);

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

    private PaymentResult executeProvider(String providerName, double amount, String country, String operator, String phone,
            String firstname, String lastname, String email) {
        if (providerName == null)
            return null;

        try {
            switch (providerName.toUpperCase()) {
                case "KKIAPAY":
                    return kkiapayClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
                case "PAYDUNYA":
                    return payDunyaClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
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

    private void saveLog(String paymentId, String providerUsed, PaymentResult result, boolean success,
            String failureReason, ErrorType errorType, boolean fallbackUsed, String fallbackReason,
            String primaryProvider) {
        try {
            LogEntry logEntry = new LogEntry();
            logEntry.setPaymentId(paymentId);
            logEntry.setRouteUsed(providerUsed != null ? providerUsed : "UNKNOWN");
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
            boolean wasPending = payment.getStatus() == PaymentStatus.PENDING;
            payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            if (wasPending && (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.FAILED)) {
                webhookService.sendWebhook(payment);
            }
        });
    }

    private String normalizeOperator(String op) {
        if (op == null || op.isEmpty()) return "UNKNOWN";
        String upper = op.toUpperCase().trim();
        
        if (upper.contains("MOOV")) return "MOOV";
        if (upper.contains("MTN")) return "MTN";
        if (upper.contains("WAVE")) return "WAVE";
        if (upper.contains("ORANGE") || upper.equals("OM")) return "ORANGE";
        if (upper.contains("FREE")) return "FREE";
        if (upper.contains("YAS") || upper.contains("MIXX")) return "YAS";
        if (upper.contains("TMO")) return "TMO"; // Togocel
        
        return upper;
    }
}