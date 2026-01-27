
package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.PaymentInitiateRequest;
import com.Api.Fidelitypay.controller.dto.PaymentResponseDTO;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import com.Api.Fidelitypay.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RouteRepository routeRepository;

    /**
     * Liste des moyens de paiement disponibles par pays
     */
    @GetMapping("/payment-options")
    public ResponseEntity<List<String>> getPaymentOptions(
            @RequestParam String country) {

        return ResponseEntity.ok(
                paymentService.getOptionsByCountry(country));
    }

    /**
     * Initialisation d’un paiement
     */
    @PostMapping("/payments/initiate")
    public ResponseEntity<PaymentResponseDTO> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request) {

        Payment payment = paymentService.initiatePayment(
                request.getAmount(),
                request.getCountry(),
                request.getOperator(),
                request.getPhone(),
                request.getFirstname(),
                request.getLastname(),
                request.getEmail());

        // Récupération des infos de la route utilisée
        boolean routeAvailable = false;
        String routeName = payment.getRouteName();
        String routeProvider = payment.getProvider();
        double routeLatency = 0.0;

        if (routeName != null) {
            Route route = routeRepository.findByName(routeName).orElse(null);
            if (route != null) {
                routeAvailable = route.isAvailability();
                routeLatency = route.getAvgLatency();
            }
        }

        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .payment(payment)
                .routeAvailable(routeAvailable)
                .routeName(routeName)
                .routeProvider(routeProvider)
                .routeLatency(routeLatency)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Statut d’un paiement
     */
    @GetMapping("/payments/status/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(
            @PathVariable String paymentId) {

        Payment payment = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(payment);
    }

    /**
     * Liste de tous les paiements
     */
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Liste des pays ayant des transactions
     */
    @GetMapping("/payments/countries")
    public ResponseEntity<List<String>> getPaymentCountries() {
        return ResponseEntity.ok(paymentService.getAllPaymentCountries());
    }

    /**
     * Callback pour Kkiapay
     */
    @PostMapping("/payments/callback/kkiapay")
    public ResponseEntity<Void> kkiapayCallback(
            @RequestBody com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback) {

        paymentService.processKkiapayCallback(callback);
        return ResponseEntity.ok().build();
    }
}
