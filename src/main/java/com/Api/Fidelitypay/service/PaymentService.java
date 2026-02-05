// package com.Api.Fidelitypay.service;

// import com.Api.Fidelitypay.enums.PaymentStatus;
// import com.Api.Fidelitypay.enums.LogStatus;
// import com.Api.Fidelitypay.enums.ErrorType;
// import com.Api.Fidelitypay.integration.PayDunyaClient;
// import com.Api.Fidelitypay.integration.PaymentResult;
// import com.Api.Fidelitypay.integration.KkiapayClient;
// import com.Api.Fidelitypay.model.LogEntry;
// import com.Api.Fidelitypay.model.Payment;
// import com.Api.Fidelitypay.model.Route;
// import com.Api.Fidelitypay.repository.LogEntryRepository;
// import com.Api.Fidelitypay.repository.PaymentRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.Optional;
// import java.util.UUID;

// @Service
// @Slf4j
// public class PaymentService {

//     private final PaymentRepository paymentRepository;
//     private final LogEntryRepository logEntryRepository;
//     private final RouteSelectionService routeSelectionService;
//     private final WebhookService webhookService;
//     private final KkiapayClient kkiapayClient;
//     private final PayDunyaClient payDunyaClient;

//     public PaymentService(
//             PaymentRepository paymentRepository,
//             LogEntryRepository logEntryRepository,
//             RouteSelectionService routeSelectionService,
//             WebhookService webhookService,
//             KkiapayClient kkiapayClient,
//             PayDunyaClient payDunyaClient) {
//         this.paymentRepository = paymentRepository;
//         this.logEntryRepository = logEntryRepository;
//         this.routeSelectionService = routeSelectionService;
//         this.webhookService = webhookService;
//         this.kkiapayClient = kkiapayClient;
//         this.payDunyaClient = payDunyaClient;
//     }

//     /**
//      * Options disponibles par pays
//      */
//     public List<String> getOptionsByCountry(String country) {
//         return List.of("Wave", "Moov", "Orange Money");
//     }

//     /**
//      * Initie un paiement
//      */
//     public Payment initiatePayment(double amount, String country, String operatorInput, String phone, String firstname,
//             String lastname, String email) {

//         // Normalisation : On met tout en MAJUSCULE pour éviter les erreurs (mali ->
//         // MALI)
//         String operator = (operatorInput != null) ? operatorInput.toUpperCase().trim() : "UNKNOWN";
//         String countryCode = (country != null) ? country.toUpperCase().trim() : "UNKNOWN";

//         String paymentId = UUID.randomUUID().toString();

//         // Création du paiement avec toutes les colonnes essentielles
//         Payment payment = new Payment();
//         payment.setPaymentId(paymentId);
//         payment.setOperator(operator);
//         payment.setAmount(BigDecimal.valueOf(amount));
//         payment.setCurrency("XOF");
//         payment.setStatus(PaymentStatus.PENDING);
//         payment.setCost(BigDecimal.ZERO);
//         payment.setCountry(countryCode);
//         payment.setCreatedAt(LocalDateTime.now());
//         payment.setUpdatedAt(LocalDateTime.now());

//         // Sauvegarde initiale
//         paymentRepository.save(payment);

//         // Sélection de la route primaire
//         Route primaryRoute = routeSelectionService.selectBestRoute(operator);
//         if (primaryRoute == null) {
//             log.warn("No route available for operator {}", operator);
//             payment.setStatus(PaymentStatus.FAILED);
//             payment.setRouteHealth("UNKNOWN");
//             paymentRepository.save(payment);
//             return payment;
//         }

//         Route routeUsed = primaryRoute;
//         PaymentResult result = executeRoute(primaryRoute, amount, countryCode, operator, phone, firstname, lastname,
//                 email);
//         boolean success = result != null && result.isSuccess();
//         String errorMessage = (result != null) ? result.getRawResponse() : "NO_RESPONSE";

//         // Définition de la santé de la route
//         String routeHealth = success ? "HEALTHY" : (isTechnicalError(errorMessage) ? "DOWN" : "DEGRADED");
//         payment.setRouteHealth(routeHealth);

//         // Route de fallback si nécessaire (seulement si erreur technique)
//         if (!success && isTechnicalError(errorMessage)) {
//             Route fallback = routeSelectionService.selectFallbackRoute(operator);
//             if (fallback != null && !fallback.getName().equals(primaryRoute.getName())) {
//                 PaymentResult fallbackResult = executeRoute(fallback, amount, countryCode, operator, phone, firstname,
//                         lastname, email);
//                 if (fallbackResult != null && fallbackResult.isSuccess()) {
//                     success = true;
//                     routeUsed = fallback;
//                     result = fallbackResult;
//                     payment.setRouteHealth("HEALTHY"); // La route de secours a fonctionné
//                     payment.setUsedFallback(true); // ✅ IMPORTANT
//                 }
//             }
//         }

//         // Détermination de la cause de l'échec
//         String failureReason = null;
//         if (!success) {
//             failureReason = determineFailureReason(errorMessage);
//             payment.setFailureReason(failureReason);
//             if (result != null && result.getErrorType() != null) {
//                 payment.setErrorType(result.getErrorType());
//             }
//         }

//         // Mise à jour du paiement avec résultat
//         payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
//         payment.setCost(BigDecimal.valueOf(success ? routeUsed.getCost() : 0.0));

//         // AMELIORATION : On enregistre TOUJOURS la route utilisée, même si ça a échoué
//         payment.setProvider(routeUsed.getProvider());
//         payment.setRouteName(routeUsed.getName());
//         payment.setRouteHealth(routeUsed.getStatus());

//         if (result != null) {
//             payment.setProviderPaymentId(result.getProviderId());
//             payment.setProviderResponse(result.getRawResponse());
//             payment.setPaymentUrl(result.getPaymentUrl());
//             payment.setProviderResponseTimeMs((long) result.getResponseTimeMs());
//         }

//         payment.setUpdatedAt(LocalDateTime.now());
//         paymentRepository.save(payment);

//         // Log de la transaction
//         saveLog(paymentId, routeUsed, result, success, failureReason, (result != null ? result.getErrorType() : null));

//         // Webhook si succès
//         if (success) {
//             webhookService.sendWebhook(payment);
//         }

//         return payment;
//     }
    

//     private PaymentResult executeRoute(Route route, double amount, String country, String operator, String phone,
//             String firstname, String lastname, String email) {
//         if (route == null)
//             return null;

//         switch (route.getProvider().toUpperCase()) {
//             case "KKIAPAY":
//                 return kkiapayClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
//             case "PAYDUNYA":
//                 return payDunyaClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
//             default:
//                 log.warn("Unsupported provider {}", route.getProvider());
//                 return null;
//         }
//     }

//     private void saveLog(String paymentId, Route routeUsed, PaymentResult result, boolean success,
//             String failureReason, ErrorType errorType) {
//         LogEntry log = new LogEntry();
//         log.setPaymentId(paymentId);
//         log.setRouteUsed(routeUsed != null ? routeUsed.getName() : "UNKNOWN");
//         log.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
//         log.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
//         log.setFailureReason(failureReason);
//         log.setErrorType(errorType);

//         String msg = (result != null) ? result.getRawResponse() : "No response";
//         // Sécurité : Troncature pour éviter de faire planter la DB si la colonne est
//         // trop petite
//         if (msg != null && msg.length() > 5000) {
//             msg = msg.substring(0, 4990) + "...[TRUNCATED]";
//         }
//         log.setMessage(msg);

//         logEntryRepository.save(log);
//     }

//     private String determineFailureReason(String errorMessage) {
//         if (errorMessage == null)
//             return "UNKNOWN_ERROR";
//         String error = errorMessage.toUpperCase();

//         if (error.contains("SOLDE") || error.contains("INSUFFICIENT") || error.contains("FUNDS")) {
//             return "INSUFFICIENT_FUNDS";
//         } else if (error.contains("TIMEOUT") || error.contains("TIME_OUT")) {
//             return "TIMEOUT";
//         } else if (error.contains("PHONE") || error.contains("NUMBER") || error.contains("INVALID")) {
//             return "INVALID_PHONE_NUMBER";
//         } else if (error.contains("AUTH") || error.contains("TOKEN") || error.contains("UNAUTHORIZED")) {
//             return "AUTHENTICATION_FAILED";
//         } else if (error.contains("NETWORK") || error.contains("CONNECTION") || error.contains("500")
//                 || error.contains("502") || error.contains("503")) {
//             return "NETWORK_ERROR";
//         } else if (error.contains("CANCEL")) {
//             return "CANCELLED_BY_USER";
//         } else if (error.contains("LIMIT") || error.contains("PLAFOND")) {
//             return "LIMIT_EXCEEDED";
//         }

//         return "GENERIC_FAILURE";
//     }

//     /**
//      * Récupération du statut d’un paiement
//      */
//     public Payment getPaymentStatus(String paymentId) {
//         Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
//         return paymentOpt.orElse(null);
//     }

//     /**
//      * Récupère tous les paiements
//      */
//     public List<Payment> getAllPayments() {
//         return paymentRepository.findAllByOrderByCreatedAtDesc();
//     }

//     /**
//      * Récupère la liste des pays ayant des transactions
//      */
//     public List<String> getAllPaymentCountries() {
//         return paymentRepository.findDistinctCountries();
//     }

//     /**
//      * Traitement du callback Kkiapay
//      */
//     public void processKkiapayCallback(com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback) {
//         log.info("Processing Kkiapay callback for transaction: {}", callback.getTransactionId());

//         Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(callback.getTransactionId());

//         if (paymentOpt.isPresent()) {
//             Payment payment = paymentOpt.get();
//             boolean wasPending = payment.getStatus() == PaymentStatus.PENDING;

//             payment.setStatus(callback.isPaymentSucces() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
//             payment.setUpdatedAt(LocalDateTime.now());
//             paymentRepository.save(payment);

//             if (wasPending && payment.getStatus() == PaymentStatus.SUCCESS) {
//                 webhookService.sendWebhook(payment);
//             }
//         } else {
//             log.warn("Payment not found for provider ID: {}", callback.getTransactionId());
//         }
//     }

//     private boolean isTechnicalError(String errorMessage) {
//         if (errorMessage == null)
//             return false;
//         String error = errorMessage.toUpperCase();
//         return error.contains("TIMEOUT") ||
//                 error.contains("CONNECTION") ||
//                 error.contains("500") ||
//                 error.contains("NETWORK") ||
//                 error.contains("INTERNAL SERVER ERROR");
//     }
// }
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
     * Initie un paiement
     */
    public Payment initiatePayment(double amount, String country, String operatorInput, String phone, String firstname,
            String lastname, String email) {

        // Normalisation
        String operator = (operatorInput != null) ? operatorInput.toUpperCase().trim() : "UNKNOWN";
        String countryCode = (country != null) ? country.toUpperCase().trim() : "UNKNOWN";

        String paymentId = UUID.randomUUID().toString();

        // Création du paiement
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOperator(operator);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCost(BigDecimal.ZERO);
        payment.setCountry(countryCode);
        payment.setUsedFallback(false);
        payment.setFallbackReason(null);
        payment.setAttemptCount(0);

        // Sauvegarde initiale
        paymentRepository.save(payment);

        // Sélection de la route primaire
        Route primaryRoute = routeSelectionService.selectBestRoute(operator);
        if (primaryRoute == null) {
            log.warn("No route available for operator {}", operator);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRouteHealth("UNKNOWN");
            payment.setFailureReason("NO_ROUTE_AVAILABLE");
            payment.setErrorType(ErrorType.UNKNOWN);
            paymentRepository.save(payment);
            return payment;
        }

        log.info("🎯 Primary route selected: {} for provider: {}", 
                 primaryRoute.getName(), primaryRoute.getProvider());

        Route routeUsed = primaryRoute;
        PaymentResult primaryResult = executeRoute(primaryRoute, amount, countryCode, operator, phone, firstname, lastname, email);
        
        boolean success = primaryResult != null && primaryResult.isSuccess();
        String errorMessage = (primaryResult != null) ? primaryResult.getRawResponse() : "NO_RESPONSE";
        ErrorType errorType = (primaryResult != null) ? primaryResult.getErrorType() : null;

        // Définition de la santé de la route
        String routeHealth = success ? "HEALTHY" : (isTechnicalError(errorMessage) ? "DOWN" : "DEGRADED");
        payment.setRouteHealth(routeHealth);

        // Log détaillé de la tentative primaire
        logRouteAttempt("PRIMARY", primaryRoute, success, errorMessage, errorType);

        // Route de fallback si nécessaire
        boolean fallbackUsed = false;
        String fallbackReason = null;
        String primaryProvider = primaryRoute.getProvider();
        
        if (!success && shouldTriggerFallback(errorMessage, errorType)) {
            fallbackReason = determineFallbackReason(errorMessage, errorType, primaryProvider);
            
            Route fallback = routeSelectionService.selectFallbackRoute(operator);
            
            if (fallback != null && !fallback.getName().equals(primaryRoute.getName())) {
                log.info("🔄 Primary route failed, trying fallback: {} for provider: {} | Reason: {}", 
                         fallback.getName(), fallback.getProvider(), fallbackReason);
                
                PaymentResult fallbackResult = executeRoute(fallback, amount, countryCode, operator, phone, firstname,
                        lastname, email);
                
                if (fallbackResult != null && fallbackResult.isSuccess()) {
                    success = true;
                    routeUsed = fallback;
                    primaryResult = fallbackResult;
                    fallbackUsed = true;
                    payment.setRouteHealth("HEALTHY");
                    payment.setUsedFallback(true);
                    payment.setFallbackReason(fallbackReason);
                    payment.setAttemptCount(1);
                    
                    log.info("✅ Fallback SUCCESS: {} | Provider: {} | Reason: {}", 
                             fallback.getName(), fallback.getProvider(), fallbackReason);
                } else {
                    String fallbackErrorMessage = (fallbackResult != null) ? fallbackResult.getRawResponse() : "NO_RESPONSE";
                    log.warn("❌ Fallback also failed: {} | Error: {} | Original reason: {}", 
                             fallback.getName(), fallbackErrorMessage, fallbackReason);
                }
            } else {
                log.warn("⚠️ No suitable fallback route available for operator: {}", operator);
            }
        }

        // Détermination de la cause de l'échec (si échec final)
        String failureReason = null;
        if (!success) {
            failureReason = determineFailureReason(errorMessage, errorType);
            payment.setFailureReason(failureReason);
            if (errorType != null) {
                payment.setErrorType(errorType);
            }
        }

        // Mise à jour du paiement avec résultat
        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setCost(BigDecimal.valueOf(success ? routeUsed.getCost() : 0.0));

        // Toujours enregistrer la route utilisée
        payment.setProvider(routeUsed.getProvider());
        payment.setRouteName(routeUsed.getName());
        payment.setRouteHealth(routeUsed.getStatus());

        if (primaryResult != null) {
            payment.setProviderPaymentId(primaryResult.getProviderId());
            payment.setProviderResponse(primaryResult.getRawResponse());
            payment.setPaymentUrl(primaryResult.getPaymentUrl());
            payment.setProviderResponseTimeMs((long) primaryResult.getResponseTimeMs());
            
            // Stocker l'erreur détaillée si présente
            if (primaryResult.getErrorType() != null) {
                payment.setErrorType(primaryResult.getErrorType());
            }
        }

        paymentRepository.save(payment);

        // Log de la transaction
        saveLog(paymentId, routeUsed, primaryResult, success, failureReason, errorType, fallbackUsed, fallbackReason, primaryProvider);

        // Webhook si succès
        if (success) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private PaymentResult executeRoute(Route route, double amount, String country, String operator, String phone,
            String firstname, String lastname, String email) {
        if (route == null) {
            return null;
        }

        log.debug("🚀 Executing route: {} with provider: {}", route.getName(), route.getProvider());
        
        switch (route.getProvider().toUpperCase()) {
            case "KKIAPAY":
                return kkiapayClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
            case "PAYDUNYA":
                return payDunyaClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
            default:
                log.warn("❓ Unsupported provider {}", route.getProvider());
                
                PaymentResult unsupported = new PaymentResult(false);
                unsupported.setRawResponse("UNSUPPORTED_PROVIDER: " + route.getProvider());
                unsupported.setErrorType(ErrorType.UNKNOWN);
                return unsupported;
        }
    }

    /**
     * Détermine la raison spécifique du fallback
     */
    private String determineFallbackReason(String errorMessage, ErrorType errorType, String primaryProvider) {
        if (errorType != null) {
            switch (errorType) {
                case AUTHENTICATION:
                    return primaryProvider + "_AUTHENTICATION_FAILED";
                case TIMEOUT:
                    return primaryProvider + "_TIMEOUT";
                case NETWORK:
                    return "NETWORK_CONNECTIVITY_ISSUE";
                case PROVIDER_DOWN:
                    return primaryProvider + "_SERVICE_DOWN";
                case INTERNAL_ERROR:
                    return primaryProvider + "_INTERNAL_ERROR";
                case BAD_REQUEST:
                    return primaryProvider + "_BAD_REQUEST";
                default:
                    return primaryProvider + "_TECHNICAL_ERROR";
            }
        }
        
        // Analyse du message d'erreur
        if (errorMessage == null) {
            return primaryProvider + "_UNKNOWN_ERROR";
        }
        
        String error = errorMessage.toUpperCase();
        
        if (error.contains("401") || error.contains("UNAUTHORIZED") || error.contains("AUTH")) {
            return primaryProvider + "_AUTHENTICATION_FAILED";
        } else if (error.contains("TIMEOUT") || error.contains("TIMED_OUT")) {
            return primaryProvider + "_TIMEOUT";
        } else if (error.contains("500") || error.contains("INTERNAL SERVER")) {
            return primaryProvider + "_INTERNAL_ERROR";
        } else if (error.contains("503") || error.contains("SERVICE UNAVAILABLE")) {
            return primaryProvider + "_SERVICE_DOWN";
        } else if (error.contains("NETWORK") || error.contains("CONNECTION") || error.contains("HOST")) {
            return "NETWORK_CONNECTIVITY_ISSUE";
        } else if (error.contains("SIMULATED_AUTH_ERROR")) {
            return "SIMULATED_AUTHENTICATION_FAILURE";
        }
        
        return primaryProvider + "_TECHNICAL_ERROR";
    }

    private void saveLog(String paymentId, Route routeUsed, PaymentResult result, boolean success,
            String failureReason, ErrorType errorType, boolean fallbackUsed, String fallbackReason, String primaryProvider) {
        LogEntry logEntry = new LogEntry();
        logEntry.setPaymentId(paymentId);
        logEntry.setRouteUsed(routeUsed != null ? routeUsed.getName() : "UNKNOWN");
        logEntry.setProvider(routeUsed != null ? routeUsed.getProvider() : "UNKNOWN");
        logEntry.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
        logEntry.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
        logEntry.setFailureReason(failureReason);
        logEntry.setErrorType(errorType);
        logEntry.setFallbackUsed(fallbackUsed);

        // Construire le message détaillé
        StringBuilder messageBuilder = new StringBuilder();
        
        // Ajouter l'info sur le provider primaire
        if (fallbackUsed) {
            messageBuilder.append("Primary provider: ").append(primaryProvider).append(" failed. ");
        }
        
        // Ajouter la réponse du provider
        if (result != null && result.getRawResponse() != null) {
            messageBuilder.append("Provider response: ").append(result.getRawResponse());
        }
        
        // Ajouter la raison du fallback
        if (fallbackUsed && fallbackReason != null) {
            messageBuilder.append(" | Fallback triggered because: ").append(fallbackReason);
        }
        
        // Ajouter la raison de l'échec
        if (!success && failureReason != null) {
            messageBuilder.append(" | Failure reason: ").append(failureReason);
        }
        
        String message = messageBuilder.toString();
        if (message.length() > 5000) {
            message = message.substring(0, 4990) + "...[TRUNCATED]";
        }
        logEntry.setMessage(message);

        logEntryRepository.save(logEntry);
        
        log.info("📝 Log saved for payment: {} | success: {} | fallback: {} | fallbackReason: {}", 
                 paymentId, success, fallbackUsed, fallbackReason);
    }

    private String determineFailureReason(String errorMessage, ErrorType errorType) {
        if (errorMessage == null) {
            return "UNKNOWN_ERROR";
        }
        
        // D'abord vérifier le type d'erreur
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
                default:
                    // Continuer avec l'analyse du message
            }
        }
        
        String error = errorMessage.toUpperCase();

        if (error.contains("SOLDE") || error.contains("INSUFFICIENT") || error.contains("FUNDS")) {
            return "INSUFFICIENT_FUNDS";
        } else if (error.contains("TIMEOUT") || error.contains("TIME_OUT") || error.contains("TIMED_OUT")) {
            return "TIMEOUT";
        } else if (error.contains("PHONE") || error.contains("NUMBER") || error.contains("INVALID")) {
            return "INVALID_PHONE_NUMBER";
        } else if (error.contains("AUTH") || error.contains("TOKEN") || error.contains("UNAUTHORIZED") 
                   || error.contains("401") || error.contains("API_KEY")) {
            return "AUTHENTICATION_FAILED";
        } else if (error.contains("NETWORK") || error.contains("CONNECTION") || error.contains("500")
                || error.contains("502") || error.contains("503") || error.contains("HOST")) {
            return "NETWORK_ERROR";
        } else if (error.contains("CANCEL") || error.contains("ABORT")) {
            return "CANCELLED_BY_USER";
        } else if (error.contains("LIMIT") || error.contains("PLAFOND") || error.contains("MAXIMUM")) {
            return "LIMIT_EXCEEDED";
        } else if (error.contains("SIMULATED_AUTH_ERROR")) {
            return "AUTHENTICATION_FAILED_SIMULATED";
        }

        return "GENERIC_FAILURE";
    }

    /**
     * Vérifie si c'est une erreur technique qui doit déclencher le fallback
     */
    private boolean shouldTriggerFallback(String errorMessage, ErrorType errorType) {
        if (errorType != null) {
            // Ces erreurs techniques déclenchent le fallback
            return errorType == ErrorType.AUTHENTICATION ||
                   errorType == ErrorType.TIMEOUT ||
                   errorType == ErrorType.NETWORK ||
                   errorType == ErrorType.PROVIDER_DOWN ||
                   errorType == ErrorType.INTERNAL_ERROR;
        }
        
        return isTechnicalError(errorMessage);
    }

    /**
     * Détecte les erreurs d'authentification
     */
    private boolean isAuthenticationError(String errorMessage) {
        if (errorMessage == null) return false;
        String error = errorMessage.toUpperCase();
        return error.contains("401") ||
               error.contains("UNAUTHORIZED") ||
               error.contains("AUTHENTICATION") ||
               error.contains("AUTH_") ||
               error.contains("TOKEN") ||
               error.contains("API KEY") ||
               error.contains("CREDENTIAL") ||
               error.contains("INVALID_KEY") ||
               error.contains("FORBIDDEN") ||
               error.contains("403") ||
               error.contains("SIMULATED_AUTH_ERROR");
    }

    /**
     * Détecte les erreurs techniques (pour le fallback)
     */
    private boolean isTechnicalError(String errorMessage) {
        if (errorMessage == null) return false;
        return isAuthenticationError(errorMessage) ||
               errorMessage.toUpperCase().contains("TIMEOUT") ||
               errorMessage.toUpperCase().contains("CONNECTION") ||
               errorMessage.toUpperCase().contains("500") ||
               errorMessage.toUpperCase().contains("502") ||
               errorMessage.toUpperCase().contains("503") ||
               errorMessage.toUpperCase().contains("504") ||
               errorMessage.toUpperCase().contains("NETWORK") ||
               errorMessage.toUpperCase().contains("INTERNAL SERVER ERROR") ||
               errorMessage.toUpperCase().contains("SOCKET") ||
               errorMessage.toUpperCase().contains("HOST") ||
               errorMessage.toUpperCase().contains("DNS") ||
               errorMessage.toUpperCase().contains("CONNECT") ||
               errorMessage.toUpperCase().contains("REFUSED") ||
               errorMessage.toUpperCase().contains("UNAVAILABLE");
    }

    /**
     * Log détaillé d'une tentative de route
     */
    private void logRouteAttempt(String attemptType, Route route, boolean success, 
                                String errorMessage, ErrorType errorType) {
        if (success) {
            log.info("✅ {} route SUCCESS: {} | provider: {}", 
                     attemptType, route.getName(), route.getProvider());
        } else {
            String errorTypeStr = (errorType != null) ? errorType.toString() : "UNKNOWN";
            String shortError = (errorMessage != null && errorMessage.length() > 200) 
                ? errorMessage.substring(0, 200) + "..." 
                : errorMessage;
            
            log.warn("❌ {} route FAILED: {} | provider: {} | type: {} | error: {}", 
                     attemptType, route.getName(), route.getProvider(), 
                     errorTypeStr, shortError);
        }
    }

    /**
     * Récupération du statut d'un paiement
     */
    public Payment getPaymentStatus(String paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        return paymentOpt.orElse(null);
    }

    /**
     * Récupère tous les paiements
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Récupère la liste des pays ayant des transactions
     */
    public List<String> getAllPaymentCountries() {
        return paymentRepository.findDistinctCountries();
    }

    /**
     * Récupère les paiements utilisant le fallback
     */
    public List<Payment> getPaymentsWithFallback() {
        return paymentRepository.findByUsedFallbackTrueOrderByCreatedAtDesc();
    }

    /**
     * Récupère les paiements avec une raison de fallback spécifique
     */
    public List<Payment> getPaymentsByFallbackReason(String reason) {
        return paymentRepository.findByUsedFallbackTrueAndFallbackReasonContainingIgnoreCaseOrderByCreatedAtDesc(reason);
    }

    /**
     * Statistiques de fallback
     */
    public FallbackStats getFallbackStats() {
        long totalPayments = paymentRepository.count();
        long fallbackPayments = paymentRepository.countByUsedFallbackTrue();
        
        FallbackStats stats = new FallbackStats();
        stats.setTotalPayments(totalPayments);
        stats.setFallbackPayments(fallbackPayments);
        stats.setFallbackRate(totalPayments > 0 ? 
            (double) fallbackPayments / totalPayments * 100 : 0);
        
        return stats;
    }

    /**
     * Traitement du callback Kkiapay
     */
    public void processKkiapayCallback(com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback) {
        log.info("Processing Kkiapay callback for transaction: {}", callback.getTransactionId());

        Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(callback.getTransactionId());

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            boolean wasPending = payment.getStatus() == PaymentStatus.PENDING;

            payment.setStatus(callback.isPaymentSucces() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            paymentRepository.save(payment);

            if (wasPending && payment.getStatus() == PaymentStatus.SUCCESS) {
                webhookService.sendWebhook(payment);
            }
            
            log.info("Callback processed for payment: {} | status: {}", 
                     payment.getPaymentId(), payment.getStatus());
        } else {
            log.warn("Payment not found for provider ID: {}", callback.getTransactionId());
        }
    }

    /**
     * Traitement du callback PayDunya
     * Note: Cette méthode est commentée car PayDunyaCallbackDTO n'existe pas encore
     * Vous pouvez la décommenter et créer la classe DTO quand vous en aurez besoin
     */
    /*
    public void processPayDunyaCallback(com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaCallbackDTO callback) {
        log.info("Processing PayDunya callback for invoice: {}", callback.getInvoiceToken());

        Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(callback.getInvoiceToken());

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            boolean wasPending = payment.getStatus() == PaymentStatus.PENDING;

            boolean isSuccess = "completed".equalsIgnoreCase(callback.getStatus()) || 
                               "paid".equalsIgnoreCase(callback.getStatus());
            
            payment.setStatus(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            paymentRepository.save(payment);

            if (wasPending && payment.getStatus() == PaymentStatus.SUCCESS) {
                webhookService.sendWebhook(payment);
            }
            
            log.info("PayDunya callback processed for payment: {} | status: {}", 
                     payment.getPaymentId(), payment.getStatus());
        } else {
            log.warn("Payment not found for PayDunya invoice token: {}", callback.getInvoiceToken());
        }
    }
    */

    /**
     * Analyse détaillée d'un paiement (pour debug/administration)
     */
    public PaymentAnalysis analyzePayment(String paymentId) {
        Payment payment = getPaymentStatus(paymentId);
        if (payment == null) {
            return null;
        }

        PaymentAnalysis analysis = new PaymentAnalysis();
        analysis.setPayment(payment);
        
        // Récupérer les logs associés
        List<LogEntry> logs = logEntryRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
        analysis.setLogs(logs);
        
        // Analyser si c'était un fallback
        if (payment.isUsedFallback()) {
            analysis.setWasFallback(true);
            analysis.setFallbackReason(payment.getFallbackReason());
            
            // Déduire le provider primaire qui a échoué
            if ("PAYDUNYA".equalsIgnoreCase(payment.getProvider())) {
                analysis.setPrimaryProvider("KKIAPAY");
                analysis.setFallbackProvider("PAYDUNYA");
            } else if ("KKIAPAY".equalsIgnoreCase(payment.getProvider())) {
                analysis.setPrimaryProvider("PAYDUNYA");
                analysis.setFallbackProvider("KKIAPAY");
            }
        }
        
        return analysis;
    }

    // Classe interne pour les statistiques
    public static class FallbackStats {
        private long totalPayments;
        private long fallbackPayments;
        private double fallbackRate;
        
        // Getters et Setters
        public long getTotalPayments() { return totalPayments; }
        public void setTotalPayments(long totalPayments) { this.totalPayments = totalPayments; }
        
        public long getFallbackPayments() { return fallbackPayments; }
        public void setFallbackPayments(long fallbackPayments) { this.fallbackPayments = fallbackPayments; }
        
        public double getFallbackRate() { return fallbackRate; }
        public void setFallbackRate(double fallbackRate) { this.fallbackRate = fallbackRate; }
    }

    // Classe interne pour l'analyse détaillée
    public static class PaymentAnalysis {
        private Payment payment;
        private List<LogEntry> logs;
        private boolean wasFallback;
        private String fallbackReason;
        private String primaryProvider;
        private String fallbackProvider;
        
        // Getters et Setters
        public Payment getPayment() { return payment; }
        public void setPayment(Payment payment) { this.payment = payment; }
        
        public List<LogEntry> getLogs() { return logs; }
        public void setLogs(List<LogEntry> logs) { this.logs = logs; }
        
        public boolean isWasFallback() { return wasFallback; }
        public void setWasFallback(boolean wasFallback) { this.wasFallback = wasFallback; }
        
        public String getFallbackReason() { return fallbackReason; }
        public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
        
        public String getPrimaryProvider() { return primaryProvider; }
        public void setPrimaryProvider(String primaryProvider) { this.primaryProvider = primaryProvider; }
        
        public String getFallbackProvider() { return fallbackProvider; }
        public void setFallbackProvider(String fallbackProvider) { this.fallbackProvider = fallbackProvider; }
    }

    /**
     * Vérifie la santé des providers
     */
    public Map<String, Boolean> checkProvidersHealth() {
        Map<String, Boolean> healthStatus = new HashMap<>();
        
        try {
            boolean kkiapayAvailable = kkiapayClient.isAvailable();
            healthStatus.put("KKIAPAY", kkiapayAvailable);
            log.info("KKIAPAY health check: {}", kkiapayAvailable ? "✅ HEALTHY" : "❌ UNHEALTHY");
        } catch (Exception e) {
            healthStatus.put("KKIAPAY", false);
            log.error("KKIAPAY health check failed", e);
        }
        
        try {
            boolean paydunyaAvailable = payDunyaClient.isAvailable();
            healthStatus.put("PAYDUNYA", paydunyaAvailable);
            log.info("PAYDUNYA health check: {}", paydunyaAvailable ? "✅ HEALTHY" : "❌ UNHEALTHY");
        } catch (Exception e) {
            healthStatus.put("PAYDUNYA", false);
            log.error("PAYDUNYA health check failed", e);
        }
        
        return healthStatus;
    }

    /**
     * Teste le fallback manuellement
     */
    public Payment testFallbackScenario(String scenario) {
        // Cette méthode est pour les tests manuels
        log.info("Testing fallback scenario: {}", scenario);
        
        // Simulation d'un paiement de test
        Payment testPayment = initiatePayment(
            1000.0, "SN", "ORANGE", "771234567", 
            "Test", "User", "test@example.com"
        );
        
        log.info("Test payment result: ID={}, Status={}, Fallback={}, Reason={}",
                 testPayment.getPaymentId(), 
                 testPayment.getStatus(),
                 testPayment.isUsedFallback(),
                 testPayment.getFallbackReason());
        
        return testPayment;
    }
}