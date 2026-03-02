package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.*;
import com.Api.Fidelitypay.service.ApiKeyService;
import com.Api.Fidelitypay.service.DeveloperMetricsService;
import com.Api.Fidelitypay.service.DeveloperWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.Api.Fidelitypay.model.User;

/**
 * Controller for Developer Portal endpoints
 * Manages API keys, metrics, activity logs, and webhooks
 */
@RestController
@RequestMapping("/api/v1/developer")
@RequiredArgsConstructor
@Slf4j
public class DeveloperController {

    private final ApiKeyService apiKeyService;
    private final DeveloperMetricsService metricsService;
    private final DeveloperWebhookService webhookService;

    // =============================================================================
    // API KEY MANAGEMENT
    // =============================================================================

    /**
     * GET /api/v1/developer/keys
     * Retrieve all API keys for the authenticated user
     */
    @GetMapping("/keys")
        public ResponseEntity<List<ApiKeyResponse>> getApiKeys(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @RequestParam(required = false) String environment) {
        String userId = resolveUserId(headerUserId);
        log.info("📋 Fetching API keys for user: {}", userId);

        List<ApiKeyResponse> keys = environment != null
            ? apiKeyService.getUserApiKeysByEnvironment(userId, environment)
            : apiKeyService.getUserApiKeys(userId);

        return ResponseEntity.ok(keys);
        }

    /**
     * POST /api/v1/developer/keys
     * Generate a new API key pair (public/secret)
     * ⚠️ WARNING: The secret key is returned ONLY ONCE - save it immediately!
     */
    @PostMapping("/keys")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @Valid @RequestBody CreateApiKeyRequest request) {
        String userId = resolveUserId(headerUserId);
        log.info("🔑 Creating new API key for user: {} in {} environment", userId, request.getEnvironment());

        ApiKeyResponse response = apiKeyService.createApiKey(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/v1/developer/keys/{id}/revoke
     * Revoke (disable) a specific API key
     */
    @PostMapping("/keys/{id}/revoke")
    public ResponseEntity<Map<String, String>> revokeApiKey(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @PathVariable String id) {
        String userId = resolveUserId(headerUserId);
        log.info("🔒 Revoking API key: {} for user: {}", id, userId);

        try {
            apiKeyService.revokeApiKey(userId, id);
            return ResponseEntity.ok(Map.of(
                    "message", "API key revoked successfully",
                    "keyId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/v1/developer/keys/rotate
     * Rotate all API keys (revoke old ones and generate new ones)
     * ⚠️ This is a sensitive operation - use with caution!
     */
    @PostMapping("/keys/rotate")
        public ResponseEntity<Map<String, Object>> rotateApiKeys(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId) {
        String userId = resolveUserId(headerUserId);
        log.warn("🔄 Rotating all API keys for user: {}", userId);

        List<ApiKeyResponse> newKeys = apiKeyService.rotateApiKeys(userId);

        return ResponseEntity.ok(Map.of(
            "message", "All API keys have been rotated successfully",
            "newKeys", newKeys));
        }

    // =============================================================================
    // METRICS & MONITORING
    // =============================================================================

    /**
     * GET /api/v1/developer/metrics
     * Get comprehensive API usage metrics for a time period
     */
    @GetMapping("/metrics")
    public ResponseEntity<DeveloperMetricsResponse> getMetrics(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @RequestParam(defaultValue = "last_24h") String period) {
        String userId = resolveUserId(headerUserId);
        log.info("📊 Fetching metrics for user: {} (period: {})", userId, period);

        DeveloperMetricsResponse metrics = metricsService.getMetrics(userId, period);

        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/v1/developer/activity
     * Get recent API activity/logs
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ApiActivityResponse>> getActivity(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "all") String filter) {
        String userId = resolveUserId(headerUserId);
        log.info("📜 Fetching activity for user: {} (limit: {}, filter: {})", userId, limit, filter);

        // Validate limit
        if (limit < 1 || limit > 100) {
            limit = 10;
        }

        // Validate filter
        if (!List.of("all", "errors", "slow").contains(filter)) {
            filter = "all";
        }

        List<ApiActivityResponse> activity = metricsService.getActivity(userId, limit, filter);

        return ResponseEntity.ok(activity);
    }

    // =============================================================================
    // WEBHOOK MANAGEMENT
    // =============================================================================

    /**
     * GET /api/v1/developer/webhooks
     * Get all configured webhooks
     */
    @GetMapping("/webhooks")
        public ResponseEntity<List<WebhookResponse>> getWebhooks(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @RequestParam(required = false) String event) {
        String userId = resolveUserId(headerUserId);
        log.info("🪝 Fetching webhooks for user: {}", userId);

        List<WebhookResponse> webhooks = event != null
            ? webhookService.getUserWebhooksByEvent(userId, event)
            : webhookService.getUserWebhooks(userId);

        return ResponseEntity.ok(webhooks);
        }

    /**
     * POST /api/v1/developer/webhooks
     * Create a new webhook endpoint
     */
    @PostMapping("/webhooks")
    public ResponseEntity<WebhookResponse> createWebhook(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @Valid @RequestBody CreateWebhookRequest request) {
        String userId = resolveUserId(headerUserId);
        log.info("🪝 Creating webhook for user: {} - Event: {}", userId, request.getEvent());

        try {
            WebhookResponse webhook = webhookService.createWebhook(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(webhook);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * PATCH /api/v1/developer/webhooks/{id}
     * Update webhook status (activate/deactivate)
     */
    @PatchMapping("/webhooks/{id}")
    public ResponseEntity<WebhookResponse> updateWebhook(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        String userId = resolveUserId(headerUserId);
        boolean isActive = body.getOrDefault("isActive", true);

        log.info("🔄 Updating webhook {} for user: {} - Active: {}", id, userId, isActive);

        try {
            WebhookResponse webhook = webhookService.updateWebhookStatus(userId, id, isActive);
            return ResponseEntity.ok(webhook);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * DELETE /api/v1/developer/webhooks/{id}
     * Delete a webhook
     */
    @DeleteMapping("/webhooks/{id}")
    public ResponseEntity<Map<String, String>> deleteWebhook(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "") String headerUserId,
            @PathVariable String id) {
        String userId = resolveUserId(headerUserId);
        log.info("🗑️ Deleting webhook {} for user: {}", id, userId);

        try {
            webhookService.deleteWebhook(userId, id);
            return ResponseEntity.ok(Map.of(
                    "message", "Webhook deleted successfully",
                    "webhookId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Helper: resolve user id from security context (JWT) or fallback to provided header
    private String resolveUserId(String headerUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u.getId();
        }
        return (headerUserId != null && !headerUserId.isBlank()) ? headerUserId : "demo-user";
    }

    // =============================================================================
    // HEALTH CHECK
    // =============================================================================

    /**
     * GET /api/v1/developer/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "Developer Portal API",
                "version", "1.0.0"));
    }
}
