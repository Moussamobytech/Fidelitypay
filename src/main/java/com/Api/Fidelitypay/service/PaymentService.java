package com.Api.Fidelitypay.service;  // Updated to match your package path

import com.Api.Fidelitypay.model.LogEntry;  // Adjust if model package differs
import com.Api.Fidelitypay.model.Payment;   // Adjust if needed
import com.Api.Fidelitypay.model.Route;      // Adjust if needed
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;    // Adjust package

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private RouteSelectionService routeSelectionService;

    @Autowired
    private WebhookService webhookService;

    public List<String> getOptionsByCountry(String country) {
        // Logique pour récupérer options depuis DB ou config
        // Pour MVP, hardcode ou de DB
        return List.of("OM", "Wave", "Moov"); // Exemple
    }

    public Payment initiatePayment(double amount, String country, String operator) {
        String paymentId = UUID.randomUUID().toString();
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOperator(operator);
        payment.setStatus("PENDING");
        payment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(payment);

        // Sélectionner route
        Route bestRoute = routeSelectionService.selectBestRoute(operator);

        try {
            // Exécuter paiement via integration (ex: appel HTTP)
            // Simuler pour MVP
            double responseTime = Math.random() * 1000; // ms
            payment.setStatus("SUCCESS");
            payment.setCost(bestRoute.getCost());

            // Log
            LogEntry log = new LogEntry();
            log.setPaymentId(paymentId);
            log.setRouteUsed(bestRoute.getName());
            log.setResponseTime(responseTime);
            log.setStatus("SUCCESS");
            log.setTimestamp(LocalDateTime.now());
            logEntryRepository.save(log);  // This line (63) should now resolve if repository is correct

            // Webhook
            webhookService.sendWebhook(payment);

        } catch (Exception e) {
            // Fallback
            Route fallbackRoute = routeSelectionService.selectFallbackRoute(operator);
            if (fallbackRoute != null) {
                // Réessayer
                // Simuler
                payment.setStatus("SUCCESS");
            } else {
                payment.setStatus("FAILED");
            }
        }

        paymentRepository.save(payment);
        return payment;
    }

    public Payment getPaymentStatus(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }
}