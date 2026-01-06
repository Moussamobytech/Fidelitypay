package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.Enum.PaymentStatus;
import com.Api.Fidelitypay.Enum.LogStatus;
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
        return List.of("Kkiapay", "Dunyapay", "Wave", "Moov", "Orange Money");
    }

    /**
     * Initie un paiement
     */
    public Payment initiatePayment(double amount, String country, String operatorInput, String phone) {

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
            paymentRepository.save(payment);
            return payment;
        }

        Route routeUsed = primaryRoute;
        PaymentResult result = executeRoute(primaryRoute, amount, countryCode, operator, phone);
        boolean success = result != null && result.isSuccess();

        // Route de fallback si nécessaire
        if (!success) {
            Route fallback = routeSelectionService.selectFallbackRoute(operator);
            if (fallback != null && !fallback.getName().equals(primaryRoute.getName())) {
                result = executeRoute(fallback, amount, countryCode, operator, phone);
                if (result != null && result.isSuccess()) {
                    success = true;
                    routeUsed = fallback;
                }
            }
        }

        // Mise à jour du paiement avec résultat
        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setCost(BigDecimal.valueOf(success ? routeUsed.getCost() : 0.0));

        // AMELIORATION : On enregistre TOUJOURS la route utilisée, même si ça a échoué
        payment.setProvider(routeUsed.getProvider());
        payment.setRouteName(routeUsed.getName());

        if (result != null) {
            payment.setProviderPaymentId(result.getProviderId());
            payment.setProviderResponse(result.getRawResponse());
            payment.setPaymentUrl(result.getPaymentUrl());
            payment.setProviderResponseTimeMs((long) result.getResponseTimeMs());
        }

        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Log de la transaction
        saveLog(paymentId, routeUsed, result, success);

        // Webhook si succès
        if (success) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private PaymentResult executeRoute(Route route, double amount, String country, String operator, String phone) {
        if (route == null)
            return null;

        switch (route.getProvider().toUpperCase()) {
            case "KKIAPAY":
                return kkiapayClient.initiatePayment(amount, country, operator, phone);
            case "PAYDUNYA":
                return payDunyaClient.initiatePayment(amount, country, operator, phone);
            default:
                log.warn("Unsupported provider {}", route.getProvider());
                return null;
        }
    }

    private void saveLog(String paymentId, Route routeUsed, PaymentResult result, boolean success) {
        LogEntry log = new LogEntry();
        log.setPaymentId(paymentId);
        log.setRouteUsed(routeUsed != null ? routeUsed.getName() : "UNKNOWN");
        log.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
        log.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
        logEntryRepository.save(log);
    }

    /**
     * Récupération du statut d’un paiement
     */
    public Payment getPaymentStatus(String paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        return paymentOpt.orElse(null);
    }
}
