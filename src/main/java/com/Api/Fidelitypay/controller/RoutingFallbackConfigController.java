package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.FallbackSettingsDto;
import com.Api.Fidelitypay.service.RoutingFallbackConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin endpoint for reading and updating the global routing fallback configuration.
 * Matches the Angular frontend endpoint: /api/v1/admin/routing/fallback-settings
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/routing")
public class RoutingFallbackConfigController {

    private final RoutingFallbackConfigService service;

    /**
     * GET /api/v1/admin/routing/fallback-settings
     * Returns current fallback configuration (creates defaults on first call).
     */
    @GetMapping("/fallback-settings")
    public ResponseEntity<FallbackSettingsDto> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    /**
     * PUT /api/v1/admin/routing/fallback-settings
     * Saves the fallback configuration.
     * Body: { autoFallback, smartRetry, retryMaxAttempts, criticalTimeoutSeconds }
     */
    @PutMapping("/fallback-settings")
    public ResponseEntity<FallbackSettingsDto> updateSettings(@RequestBody FallbackSettingsDto dto) {
        return ResponseEntity.ok(service.updateSettings(dto));
    }
}
