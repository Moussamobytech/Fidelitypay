package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.PaymentProviderRequest;
import com.Api.Fidelitypay.controller.dto.PaymentProviderResponse;
import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteRequest;
import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteResponse;
import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import com.Api.Fidelitypay.service.PaymentProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentProviderController {

    private final PaymentProviderService paymentProviderService;

    @GetMapping("/api/v1/admin/payment-providers")
    public List<PaymentProviderResponse> listAdminProviders() {
        return paymentProviderService.listProviders(false);
    }

    @PostMapping("/api/v1/admin/payment-providers")
    public ResponseEntity<PaymentProviderResponse> createProvider(@RequestBody PaymentProviderRequest request) {
        return ResponseEntity.ok(paymentProviderService.createProvider(request));
    }

    @PutMapping("/api/v1/admin/payment-providers/{id}")
    public ResponseEntity<PaymentProviderResponse> updateProvider(@PathVariable Long id,
            @RequestBody PaymentProviderRequest request) {
        return ResponseEntity.ok(paymentProviderService.updateProvider(id, request));
    }

    @PatchMapping("/api/v1/admin/payment-providers/{id}/status")
    public ResponseEntity<PaymentProviderResponse> setProviderStatus(@PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        PaymentProviderStatus status = PaymentProviderStatus.valueOf(payload.getOrDefault("status", "ACTIVE"));
        return ResponseEntity.ok(paymentProviderService.setProviderStatus(id, status));
    }

    @DeleteMapping("/api/v1/admin/payment-providers/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        paymentProviderService.deleteProvider(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/developer/payment-providers")
    public List<PaymentProviderResponse> listMerchantProviders() {
        return paymentProviderService.listProviders(true);
    }

    @GetMapping("/api/v1/admin/payment-provider-routes")
    public List<PaymentProviderRouteResponse> listAdminRoutes() {
        return paymentProviderService.listRoutes();
    }

    @PostMapping("/api/v1/admin/payment-provider-routes")
    public ResponseEntity<PaymentProviderRouteResponse> createRoute(@RequestBody PaymentProviderRouteRequest request) {
        return ResponseEntity.ok(paymentProviderService.createRoute(request));
    }

    @PutMapping("/api/v1/admin/payment-provider-routes/{id}")
    public ResponseEntity<PaymentProviderRouteResponse> updateRoute(@PathVariable Long id,
            @RequestBody PaymentProviderRouteRequest request) {
        return ResponseEntity.ok(paymentProviderService.updateRoute(id, request));
    }

    @PatchMapping("/api/v1/admin/payment-provider-routes/{id}/status")
    public ResponseEntity<PaymentProviderRouteResponse> setRouteEnabled(@PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {
        return ResponseEntity.ok(paymentProviderService.setRouteEnabled(id, payload.getOrDefault("enabled", true)));
    }

    @DeleteMapping("/api/v1/admin/payment-provider-routes/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        paymentProviderService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
