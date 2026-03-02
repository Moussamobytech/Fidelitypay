
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.Api.Fidelitypay.model.User;
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

        // Récupérer l'utilisateur authentifié
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Payment payment = paymentService.initiatePayment(
                user,
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        Payment payment = paymentService.getPaymentStatus(paymentId);

        if (payment != null && user.getRole() != User.Role.ADMIN) {
            // Si ce n'est pas un admin, on vérifie que le paiement appartient à
            // l'utilisateur
            if (payment.getUser() == null || !payment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(payment);
    }

    /**
     * Liste de tous les paiements
     */
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments(@RequestParam(required = false) String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        if (userId != null && user.getRole() == User.Role.ADMIN) {
            return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
        }

        return ResponseEntity.ok(paymentService.getAllPayments(user));
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
