package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.CreateWebhookRequest;
import com.Api.Fidelitypay.controller.dto.WebhookResponse;
import com.Api.Fidelitypay.model.Webhook;
import com.Api.Fidelitypay.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user-configured webhooks
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeveloperWebhookService {

    private final WebhookRepository webhookRepository;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Get all webhooks for a user
     */
    public List<WebhookResponse> getUserWebhooks(String userId) {
        List<Webhook> webhooks = webhookRepository.findByUserId(userId);
        return webhooks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get webhooks by event type
     */
    public List<WebhookResponse> getUserWebhooksByEvent(String userId, String event) {
        List<Webhook> webhooks = webhookRepository.findByUserIdAndEvent(userId, event);
        return webhooks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new webhook
     */
    @Transactional
    public WebhookResponse createWebhook(String userId, CreateWebhookRequest request) {
        // Check if webhook already exists
        if (webhookRepository.existsByUserIdAndUrlAndEvent(userId, request.getUrl(), request.getEvent())) {
            throw new IllegalArgumentException("Webhook already exists for this URL and event");
        }

        // Generate secret for HMAC signature
        String secret = generateWebhookSecret();

        Webhook webhook = Webhook.builder()
                .userId(userId)
                .url(request.getUrl())
                .event(request.getEvent())
                .description(request.getDescription())
                .secret(secret)
                .isActive(true)
                .failureCount(0)
                .build();

        Webhook saved = webhookRepository.save(webhook);
        log.info("✅ Created webhook for user {} - Event: {}", userId, request.getEvent());

        return toResponse(saved);
    }

    /**
     * Update webhook status (activate/deactivate)
     */
    @Transactional
    public WebhookResponse updateWebhookStatus(String userId, String webhookId, boolean isActive) {
        Webhook webhook = webhookRepository.findByIdAndUserId(webhookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found"));

        webhook.setActive(isActive);
        webhook.setUpdatedAt(LocalDateTime.now());
        Webhook updated = webhookRepository.save(webhook);

        log.info("{} webhook {} for user {}", isActive ? "🟢 Activated" : "🔴 Deactivated", webhookId, userId);

        return toResponse(updated);
    }

    /**
     * Delete a webhook
     */
    @Transactional
    public void deleteWebhook(String userId, String webhookId) {
        Webhook webhook = webhookRepository.findByIdAndUserId(webhookId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Webhook not found"));

        webhookRepository.delete(webhook);
        log.info("🗑️ Deleted webhook {} for user {}", webhookId, userId);
    }

    /**
     * Get active webhooks for a specific event (used when triggering webhooks)
     */
    public List<Webhook> getActiveWebhooksForEvent(String userId, String event) {
        return webhookRepository.findByUserIdAndEventAndIsActive(userId, event, true);
    }

    /**
     * Update webhook after trigger attempt
     */
    @Transactional
    public void updateWebhookTrigger(String webhookId, int statusCode, boolean success) {
        webhookRepository.findById(webhookId).ifPresent(webhook -> {
            webhook.setLastTriggeredAt(LocalDateTime.now());
            webhook.setLastStatusCode(statusCode);
            webhook.setUpdatedAt(LocalDateTime.now());

            if (success) {
                webhook.setFailureCount(0); // Reset failure count on success
            } else {
                webhook.setFailureCount(webhook.getFailureCount() + 1);

                // Auto-disable after 10 consecutive failures
                if (webhook.getFailureCount() >= 10) {
                    webhook.setActive(false);
                    log.warn("⚠️ Auto-disabled webhook {} after 10 consecutive failures", webhookId);
                }
            }

            webhookRepository.save(webhook);
        });
    }

    /**
     * Generate a secure webhook secret for HMAC signing
     */
    private String generateWebhookSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Convert Webhook entity to response DTO
     */
    private WebhookResponse toResponse(Webhook webhook) {
        return WebhookResponse.builder()
                .id(webhook.getId())
                .url(webhook.getUrl())
                .event(webhook.getEvent())
                .description(webhook.getDescription())
                .isActive(webhook.isActive())
                .lastTriggeredAt(webhook.getLastTriggeredAt())
                .lastStatusCode(webhook.getLastStatusCode())
                .failureCount(webhook.getFailureCount())
                .createdAt(webhook.getCreatedAt())
                .build();
    }
}
