package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteResponse;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.service.MerchantPaymentRouteSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentRouteSettingsController {

    private final MerchantPaymentRouteSettingService routeSettingService;

    @GetMapping("/api/v1/developer/payment-routes")
    public List<PaymentProviderRouteResponse> listMerchantRoutes(Authentication authentication) {
        return routeSettingService.listMerchantPayInRoutes(resolveUserId(authentication));
    }

    @PatchMapping("/api/v1/developer/payment-routes/{routeId}/status")
    public ResponseEntity<PaymentProviderRouteResponse> setMerchantRouteEnabled(Authentication authentication,
            @PathVariable Long routeId, @RequestBody Map<String, Boolean> payload) {
        return ResponseEntity.ok(routeSettingService.setMerchantRouteEnabled(resolveUserId(authentication), routeId,
                payload.getOrDefault("enabled", true)));
    }

    @GetMapping("/api/v1/admin/payment-routes")
    public List<PaymentProviderRouteResponse> listPlatformRoutes() {
        return routeSettingService.listPlatformPayInRoutes();
    }

    @PatchMapping("/api/v1/admin/payment-routes/{routeId}/status")
    public ResponseEntity<PaymentProviderRouteResponse> setPlatformRouteEnabled(@PathVariable Long routeId,
            @RequestBody Map<String, Boolean> payload) {
        return ResponseEntity.ok(routeSettingService.setPlatformRouteEnabled(routeId,
                payload.getOrDefault("enabled", true)));
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new AccessDeniedException("Authenticated user required");
    }
}
