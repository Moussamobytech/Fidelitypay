package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.SamirPayClient;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.Enum.PaymentStatus;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LogEntryRepository logEntryRepository;
    private final RouteSelectionService routeSelectionService;
    private final WebhookService webhookService;
    private final SamirPayClient samirPayClient;
    private final PayDunyaClient payDunyaClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            LogEntryRepository logEntryRepository,
            RouteSelectionService routeSelectionService,
            WebhookService webhookService,
            SamirPayClient samirPayClient,
            PayDunyaClient payDunyaClient) {
        this.paymentRepository = paymentRepository;
        this.logEntryRepository = logEntryRepository;
        this.routeSelectionService = routeSelectionService;
        this.webhookService = webhookService;
        this.samirPayClient = samirPayClient;
        this.payDunyaClient = payDunyaClient;
    }

    public List<String> getOptionsByCountry(String country) {
        return List.of("OM", "Wave", "Moov", "Orange Money");
    }

    public Payment initiatePayment(double amount, String country, String operator) {

        String paymentId = UUID.randomUUID().toString();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOperator(operator);
        payment.setAmount(amount);
        payment.setCurrency("XOF");
        payment.setStatus(PaymentStatus.PENDING);
        // Timestamp is handled by @CreationTimestamp in Payment entity
        paymentRepository.save(payment);

        Route primaryRoute = routeSelectionService.selectBestRoute(operator);

        if (primaryRoute == null) {
            log.warn("No route available for operator {}", operator);
            payment.setStatus(PaymentStatus.FAILED);
            return paymentRepository.save(payment);
        }

        boolean success = executeRoute(primaryRoute, amount, country, operator);
        Route routeUsed = primaryRoute;

        if (!success) {
            log.warn("Primary route {} failed, trying fallback", primaryRoute.getName());
            Route fallback = routeSelectionService.selectFallbackRoute(operator);
            if (fallback != null && !fallback.getName().equals(primaryRoute.getName())) {
                success = executeRoute(fallback, amount, country, operator);
                if (success) {
                    routeUsed = fallback;
                }
            }
        }

        payment.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setCost(success ? routeUsed.getCost() : 0.0);
        paymentRepository.save(payment);

        saveLog(paymentId, routeUsed.getName(), success);

        if (success) {
            webhookService.sendWebhook(payment);
        }

        return payment;
    }

    private boolean executeRoute(Route route, double amount, String country, String operator) {
        if (route.getName().contains("SamirPay")) {
            return samirPayClient.initiatePayment(amount, country, operator);
        }
        if (route.getName().contains("PayDunya")) {
            return payDunyaClient.initiatePayment(amount, country, operator);
        }
        return false;
    }

    private void saveLog(String paymentId, String routeUsed, boolean success) {
        LogEntry log = new LogEntry();
        log.setPaymentId(paymentId);
        log.setRouteUsed(routeUsed);
        log.setResponseTime(120.0);
        log.setStatus(success ? "SUCCESS" : "FAILED");
        log.setTimestamp(LocalDateTime.now());
        logEntryRepository.save(log);
    }

    public Payment getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
}