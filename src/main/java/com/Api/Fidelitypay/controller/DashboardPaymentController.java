package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.MerchantApiPrincipal;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentRequest;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentResponse;
import com.Api.Fidelitypay.controller.dto.PaymentInitiateRequest;
import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.ApiKeyRepository;
import com.Api.Fidelitypay.service.MerchantPayInService;
import com.Api.Fidelitypay.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.Api.Fidelitypay.model.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "dashboard-payment-tester", description = "JWT-authenticated dashboard payment tester and payment history")
public class DashboardPaymentController {

    private final PaymentService paymentService;
    private final MerchantPayInService merchantPayInService;
    private final ApiKeyRepository apiKeyRepository;

    /**
     * Liste des moyens de paiement disponibles par pays
     */
    @GetMapping("/payment-options")
    @Operation(summary = "List payment operators for the dashboard test form")
    public ResponseEntity<List<String>> getPaymentOptions(
            @RequestParam String country) {

        return ResponseEntity.ok(
                paymentService.getOptionsByCountry(country));
    }

    /**
     * Dashboard manual payment test. Uses the authenticated merchant account and an
     * active API key, while the provider environment is selected in the test form.
     */
    @PostMapping("/payments/initiate")
    @Operation(summary = "Initiate a dashboard test payment", description = "Session/JWT endpoint used by the dashboard test tool. Public merchant integrations should use /api/v1/payments/initiate.")
    public ResponseEntity<MerchantPaymentResponse> initiateDashboardTestPayment(
            @Valid @RequestBody PaymentInitiateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        String environment = normalizeEnvironment(request.getEnvironment());
        ApiKey apiKey = apiKeyRepository
                .findByUserIdAndIsActive(user.getId(), true)
                .stream()
                .findFirst()
                .orElse(null);

        if (apiKey == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Create an active API key before using the dashboard payment tester");
        }

        MerchantPaymentRequest merchantRequest = new MerchantPaymentRequest();
        merchantRequest.setAmount(request.getAmount().longValue());
        merchantRequest.setCurrency("XOF");
        merchantRequest.setCountry(request.getCountry());
        merchantRequest.setOperator(request.getOperator());
        merchantRequest.setReturnUrl(request.getReturnUrl());
        merchantRequest.setCancelUrl(request.getCancelUrl());

        MerchantPaymentRequest.Customer customer = new MerchantPaymentRequest.Customer();
        customer.setPhone(request.getPhone());
        customer.setFirstname(defaultIfBlank(request.getFirstname(), "Dashboard"));
        customer.setLastname(defaultIfBlank(request.getLastname(), "Tester"));
        customer.setEmail(request.getEmail());
        merchantRequest.setCustomer(customer);

        MerchantApiPrincipal principal = MerchantApiPrincipal.builder()
                .apiKey(apiKey)
                .user(user)
                .environment(environment)
                .initiationSource("DASHBOARD")
                .build();

        String idempotencyKey = defaultIfBlank(request.getIdempotencyKey(), "dashboard-" + UUID.randomUUID());
        return ResponseEntity.ok(merchantPayInService.initiate(principal, merchantRequest, idempotencyKey));
    }

    /**
     * Statut d’un paiement
     */
    @GetMapping("/payments/status/{paymentId}")
    @Operation(summary = "Read a dashboard payment status")
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
    @Operation(summary = "List dashboard payments")
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
    @Operation(summary = "List countries found in dashboard payments")
    public ResponseEntity<List<String>> getPaymentCountries() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();

        if (user.getRole() == User.Role.ADMIN) {
            return ResponseEntity.ok(paymentService.getAllPaymentCountries());
        }
        return ResponseEntity.ok(paymentService.getPaymentCountriesByUser(user));
    }

    private String normalizeEnvironment(String environment) {
        if (environment == null || environment.isBlank()) {
            return "LIVE";
        }
        String normalized = environment.trim().toUpperCase();
        if (!"LIVE".equals(normalized) && !"SANDBOX".equals(normalized)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "environment must be LIVE or SANDBOX");
        }
        return normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
