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
import java.net.URI;
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
        validatePublicHttpsUrl(request.getUrl());
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

        return toResponseWithSecret(saved);
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

    private void validatePublicHttpsUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || host.isBlank()) {
                throw new IllegalArgumentException("Webhook URL must be a valid HTTPS URL");
            }
            String normalizedHost = host.toLowerCase();
            if (normalizedHost.equals("localhost")
                    || normalizedHost.endsWith(".localhost")
                    || normalizedHost.endsWith(".local")
                    || normalizedHost.endsWith(".internal")
                    || isPrivateIpv4(normalizedHost)
                    || isPrivateIpv6(normalizedHost)) {
                throw new IllegalArgumentException("Webhook URL must use a public Internet host");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Webhook URL must be a valid public HTTPS URL", exception);
        }
    }

    private boolean isPrivateIpv4(String host) {
        if (!host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return false;
        String[] parts = host.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        return first == 0 || first == 10 || first == 127
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168)
                || first >= 224;
    }

    private boolean isPrivateIpv6(String host) {
        String normalized = host.toLowerCase();
        return normalized.equals("::") || normalized.equals("::1")
                || normalized.startsWith("fc") || normalized.startsWith("fd")
                || normalized.startsWith("fe8") || normalized.startsWith("fe9")
                || normalized.startsWith("fea") || normalized.startsWith("feb");
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

    private WebhookResponse toResponseWithSecret(Webhook webhook) {
        WebhookResponse response = toResponse(webhook);
        response.setSecret(webhook.getSecret());
        return response;
    }
}
