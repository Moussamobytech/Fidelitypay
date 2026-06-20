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
     * Retrieve active API keys for the authenticated user
     */
    @GetMapping("/keys")
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys() {
        String userId = resolveUserId();
        log.info("📋 Fetching API keys for user: {}", userId);

        List<ApiKeyResponse> keys = apiKeyService.getUserApiKeys(userId);

        return ResponseEntity.ok(keys);
    }

    /**
     * POST /api/v1/developer/keys
     * Generate a new API key. Its complete value is returned only once.
     */
    @PostMapping("/keys")
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        String userId = resolveUserId();
        log.info("🔑 Creating new API key for user: {}", userId);

        ApiKeyResponse response = apiKeyService.createApiKey(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/v1/developer/keys/{id}
     * Rename a merchant API key.
     */
    @PatchMapping("/keys/{id}")
    public ResponseEntity<?> renameApiKey(
            @PathVariable String id,
            @Valid @RequestBody UpdateApiKeyRequest request) {
        String userId = resolveUserId();
        log.info("Renaming API key: {} for user: {}", id, userId);

        try {
            return ResponseEntity.ok(apiKeyService.renameApiKey(userId, id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/developer/keys/{id}
     * Remove a merchant API key from the dashboard and disable authentication.
     */
    @DeleteMapping("/keys/{id}")
    public ResponseEntity<Map<String, String>> deleteApiKey(
            @PathVariable String id) {
        String userId = resolveUserId();
        log.info("Deleting API key: {} for user: {}", id, userId);

        try {
            apiKeyService.deleteApiKey(userId, id);
            return ResponseEntity.ok(Map.of(
                    "message", "API key deleted successfully",
                    "keyId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
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
            @RequestParam(defaultValue = "last_24h") String period) {
        String userId = resolveUserId();
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
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "all") String filter) {
        String userId = resolveUserId();
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
            @RequestParam(required = false) String event) {
        String userId = resolveUserId();
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
            @Valid @RequestBody CreateWebhookRequest request) {
        String userId = resolveUserId();
        log.info("🪝 Creating webhook for user: {} - Event: {}", userId, request.getEvent());

        try {
            WebhookResponse webhook = webhookService.createWebhook(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(webhook);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(e.getMessage() != null && e.getMessage().startsWith("Webhook already")
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * PATCH /api/v1/developer/webhooks/{id}
     * Update webhook status (activate/deactivate)
     */
    @PatchMapping("/webhooks/{id}")
    public ResponseEntity<WebhookResponse> updateWebhook(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        String userId = resolveUserId();
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
            @PathVariable String id) {
        String userId = resolveUserId();
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

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u.getId();
        }
        throw new org.springframework.security.access.AccessDeniedException("Authenticated user required");
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
