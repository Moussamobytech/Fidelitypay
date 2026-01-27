package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.Enum.PaymentStatus;
import com.Api.Fidelitypay.Enum.LogStatus;
import com.Api.Fidelitypay.Enum.ErrorType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
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

        // Normalisation : On met tout en MAJUSCULE pour éviter les erreurs (mali ->
        // MALI)
        String operator = (operatorInput != null) ? operatorInput.toUpperCase().trim() : "UNKNOWN";
        String countryCode = (country != null) ? country.toUpperCase().trim() : "UNKNOWN";

        String paymentId = UUID.randomUUID().toString();

        // Création du paiement avec toutes les colonnes essentielles
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOperator(operator);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCost(BigDecimal.ZERO);
        payment.setCountry(countryCode);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // Sauvegarde initiale
        paymentRepository.save(payment);

        // Sélection de la route primaire
        Route primaryRoute = routeSelectionService.selectBestRoute(operator);
        if (primaryRoute == null) {
            log.warn("No route available for operator {}", operator);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRouteHealth("UNKNOWN");
            paymentRepository.save(payment);
            return payment;
        }

        Route routeUsed = primaryRoute;
        PaymentResult result = executeRoute(primaryRoute, amount, countryCode, operator, phone, firstname, lastname,
                email);
        boolean success = result != null && result.isSuccess();
        String errorMessage = (result != null) ? result.getRawResponse() : "NO_RESPONSE";

        // Définition de la santé de la route
        String routeHealth = success ? "HEALTHY" : (isTechnicalError(errorMessage) ? "DOWN" : "DEGRADED");
        payment.setRouteHealth(routeHealth);

        // Route de fallback si nécessaire (seulement si erreur technique)
        if (!success && isTechnicalError(errorMessage)) {
            Route fallback = routeSelectionService.selectFallbackRoute(operator);
            if (fallback != null && !fallback.getName().equals(primaryRoute.getName())) {
                PaymentResult fallbackResult = executeRoute(fallback, amount, countryCode, operator, phone, firstname,
                        lastname, email);
                if (fallbackResult != null && fallbackResult.isSuccess()) {
                    success = true;
                    routeUsed = fallback;
                    result = fallbackResult;
                    payment.setRouteHealth("HEALTHY"); // La route de secours a fonctionné
                }
            }
        }

        // Détermination de la cause de l'échec
        String failureReason = null;
        if (!success) {
            failureReason = determineFailureReason(errorMessage);
            payment.setFailureReason(failureReason);
            if (result != null && result.getErrorType() != null) {
                payment.setErrorType(result.getErrorType());
            }
        }

        // Mise à jour du paiement avec résultat
        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setCost(BigDecimal.valueOf(success ? routeUsed.getCost() : 0.0));

        // AMELIORATION : On enregistre TOUJOURS la route utilisée, même si ça a échoué
        payment.setProvider(routeUsed.getProvider());
        payment.setRouteName(routeUsed.getName());
        payment.setRouteHealth(routeUsed.getStatus());

        if (result != null) {
            payment.setProviderPaymentId(result.getProviderId());
            payment.setProviderResponse(result.getRawResponse());
            payment.setPaymentUrl(result.getPaymentUrl());
            payment.setProviderResponseTimeMs((long) result.getResponseTimeMs());
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Log de la transaction
        saveLog(paymentId, routeUsed, result, success, failureReason, (result != null ? result.getErrorType() : null));

        // Webhook si succès
        if (success) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private PaymentResult executeRoute(Route route, double amount, String country, String operator, String phone,
            String firstname, String lastname, String email) {
        if (route == null)
            return null;

        switch (route.getProvider().toUpperCase()) {
            case "KKIAPAY":
                return kkiapayClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
            case "PAYDUNYA":
                return payDunyaClient.initiatePayment(amount, country, operator, phone, firstname, lastname, email);
            default:
                log.warn("Unsupported provider {}", route.getProvider());
                return null;
        }
    }

    private void saveLog(String paymentId, Route routeUsed, PaymentResult result, boolean success,
            String failureReason, ErrorType errorType) {
        LogEntry log = new LogEntry();
        log.setPaymentId(paymentId);
        log.setRouteUsed(routeUsed != null ? routeUsed.getName() : "UNKNOWN");
        log.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
        log.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
        log.setFailureReason(failureReason);
        log.setErrorType(errorType);

        String msg = (result != null) ? result.getRawResponse() : "No response";
        // Sécurité : Troncature pour éviter de faire planter la DB si la colonne est
        // trop petite
        if (msg != null && msg.length() > 5000) {
            msg = msg.substring(0, 4990) + "...[TRUNCATED]";
        }
        log.setMessage(msg);

        logEntryRepository.save(log);
    }

    private String determineFailureReason(String errorMessage) {
        if (errorMessage == null)
            return "UNKNOWN_ERROR";
        String error = errorMessage.toUpperCase();

        if (error.contains("SOLDE") || error.contains("INSUFFICIENT") || error.contains("FUNDS")) {
            return "INSUFFICIENT_FUNDS";
        } else if (error.contains("TIMEOUT") || error.contains("TIME_OUT")) {
            return "TIMEOUT";
        } else if (error.contains("PHONE") || error.contains("NUMBER") || error.contains("INVALID")) {
            return "INVALID_PHONE_NUMBER";
        } else if (error.contains("AUTH") || error.contains("TOKEN") || error.contains("UNAUTHORIZED")) {
            return "AUTHENTICATION_FAILED";
        } else if (error.contains("NETWORK") || error.contains("CONNECTION") || error.contains("500")
                || error.contains("502") || error.contains("503")) {
            return "NETWORK_ERROR";
        } else if (error.contains("CANCEL")) {
            return "CANCELLED_BY_USER";
        } else if (error.contains("LIMIT") || error.contains("PLAFOND")) {
            return "LIMIT_EXCEEDED";
        }

        return "GENERIC_FAILURE";
    }

    /**
     * Récupération du statut d’un paiement
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
     * Traitement du callback Kkiapay
     */
    public void processKkiapayCallback(com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback) {
        log.info("Processing Kkiapay callback for transaction: {}", callback.getTransactionId());

        Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(callback.getTransactionId());

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            boolean wasPending = payment.getStatus() == PaymentStatus.PENDING;

            payment.setStatus(callback.isPaymentSucces() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            if (wasPending && payment.getStatus() == PaymentStatus.SUCCESS) {
                webhookService.sendWebhook(payment);
            }
        } else {
            log.warn("Payment not found for provider ID: {}", callback.getTransactionId());
        }
    }

    private boolean isTechnicalError(String errorMessage) {
        if (errorMessage == null)
            return false;
        String error = errorMessage.toUpperCase();
        return error.contains("TIMEOUT") ||
                error.contains("CONNECTION") ||
                error.contains("500") ||
                error.contains("NETWORK") ||
                error.contains("INTERNAL SERVER ERROR");
    }
}
