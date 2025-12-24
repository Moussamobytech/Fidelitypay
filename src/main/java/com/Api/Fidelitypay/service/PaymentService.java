
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

    // Options disponibles par pays
    public List<String> getOptionsByCountry(String country) {
        return List.of("OM", "Wave", "Moov", "Orange Money");
    }

    public Payment initiatePayment(double amount, String country, String operator) {

        String paymentId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOperator(operator);
        payment.setAmount(BigDecimal.valueOf(amount));
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCost(BigDecimal.ZERO);

        paymentRepository.save(payment);

        // Sélection de la meilleure route
        Route primaryRoute = routeSelectionService.selectBestRoute(operator);
        if (primaryRoute == null) {
            log.warn("No route available for operator {}", operator);
            payment.setStatus(PaymentStatus.FAILED);
            return paymentRepository.save(payment);
        }

        Route routeUsed = primaryRoute;
        PaymentResult result = executeRoute(primaryRoute, amount, country, operator);
        boolean success = result != null && result.isSuccess();

        // Fallback si la route primaire échoue
        if (!success) {
            Route fallback = routeSelectionService.selectFallbackRoute(operator);
            if (fallback != null && !fallback.getName().equals(primaryRoute.getName())) {
                result = executeRoute(fallback, amount, country, operator);
                if (result != null && result.isSuccess()) {
                    success = true;
                    routeUsed = fallback;
                }
            }
        }

        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setCost(BigDecimal.valueOf(success ? routeUsed.getCost() : 0.0));

        if (result != null) {
            payment.setProviderPaymentId(result.getProviderId());
            payment.setProviderResponse(result.getRawResponse());
            payment.setPaymentUrl(result.getPaymentUrl());
            payment.setProviderResponseTimeMs((long) result.getResponseTimeMs());
        }

        paymentRepository.save(payment);

        saveLog(paymentId, routeUsed, result, success);

        if (success) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

   private PaymentResult executeRoute(Route route, double amount, String country, String operator) {

    if ("KKIAPAY".equalsIgnoreCase(route.getProvider())) {
        return kkiapayClient.initiatePayment(amount, country, operator);
    }

    if ("PAYDUNYA".equalsIgnoreCase(route.getProvider())) {
        return payDunyaClient.initiatePayment(amount, country, operator);
    }

    log.warn("Unsupported provider {}", route.getProvider());
    return null;
}

    private void saveLog(String paymentId, Route routeUsed, PaymentResult result, boolean success) {
        LogEntry log = new LogEntry();
        log.setPaymentId(paymentId);
        log.setRouteUsed(routeUsed.getName());
        log.setResponseTime(result != null ? result.getResponseTimeMs() : 0.0);
        log.setStatus(success ? LogStatus.SUCCESS : LogStatus.FAILED);
        logEntryRepository.save(log);
    }

    public Payment getPaymentStatus(String paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        return paymentOpt.orElse(null);
    }
}
