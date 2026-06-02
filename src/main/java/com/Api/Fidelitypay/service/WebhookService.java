package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.Webhook;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.service.dto.WebhookDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;
    private final DeveloperWebhookService developerWebhookService;
    private final ObjectMapper objectMapper;
    private final LogEntryRepository logEntryRepository;

    public WebhookService(RestTemplate restTemplate, DeveloperWebhookService developerWebhookService,
            ObjectMapper objectMapper, LogEntryRepository logEntryRepository) {
        this.restTemplate = restTemplate;
        this.developerWebhookService = developerWebhookService;
        this.objectMapper = objectMapper;
        this.logEntryRepository = logEntryRepository;
    }

    /**
     * Envoie le paiement au webhook configuré de manière asynchrone
     */
    @Async
    public void sendWebhook(Payment payment) {
        String event = eventFor(payment);
        WebhookDTO payload = WebhookDTO.builder()
                .paymentId(payment.getPaymentId())
                .event(event)
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .operator(payment.getOperator())
                .country(payment.getCountry())
                .providerPaymentId(payment.getProviderPaymentId())
                .failureReason(payment.getFailureReason())
                .updatedAt(payment.getUpdatedAt())
                .build();

        if (payment.getUser() != null) {
            List<Webhook> webhooks = developerWebhookService.getActiveWebhooksForEvent(payment.getUser().getId(), event);
            for (Webhook webhook : webhooks) {
                sendDeveloperWebhook(webhook, payload, payment);
            }
            if (!webhooks.isEmpty()) {
                return;
            }
        }

        logWebhook(payment, "WEBHOOK_SKIPPED_NO_ENDPOINT", LogStatus.PENDING, "No active webhook configured for event " + event);
        log.warn("No developer webhook configured for event {}, skipping payment {}", event, payment.getPaymentId());
    }

    private void sendDeveloperWebhook(Webhook webhook, WebhookDTO payload, Payment payment) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-FidelityPay-Event", payload.getEvent());
            headers.set("X-FidelityPay-Signature", "sha256=" + hmacSha256(webhook.getSecret(), body));
            ResponseEntity<Void> response = restTemplate.postForEntity(webhook.getUrl(), new HttpEntity<>(body, headers), Void.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            developerWebhookService.updateWebhookTrigger(webhook.getId(), response.getStatusCode().value(), success);
            logWebhook(payment, success ? "WEBHOOK_SENT" : "WEBHOOK_REJECTED", success ? LogStatus.SUCCESS : LogStatus.FAILED,
                    "webhookId=" + webhook.getId() + " status=" + response.getStatusCode().value());
            log.info("Developer webhook {} sent for payment {}", webhook.getId(), payment.getPaymentId());
        } catch (Exception ex) {
            developerWebhookService.updateWebhookTrigger(webhook.getId(), 0, false);
            logWebhook(payment, "WEBHOOK_FAILED", LogStatus.FAILED,
                    "webhookId=" + webhook.getId() + " error=" + ex.getMessage());
            log.error("Developer webhook {} failed for payment {}: {}", webhook.getId(), payment.getPaymentId(), ex.getMessage());
        }
    }

    private void logWebhook(Payment payment, String event, LogStatus status, String message) {
        try {
            LogEntry entry = new LogEntry();
            entry.setPaymentId(payment.getPaymentId());
            entry.setRouteUsed(nonBlank(payment.getRouteName(), nonBlank(payment.getProvider(), "UNKNOWN")));
            entry.setProvider(nonBlank(payment.getProvider(), "UNKNOWN"));
            entry.setResponseTime(payment.getProviderResponseTimeMs() == null ? 0.0 : payment.getProviderResponseTimeMs().doubleValue());
            entry.setStatus(status);
            entry.setFailureReason(event);
            entry.setFallbackUsed(payment.isUsedFallback());
            entry.setMessage(message);
            logEntryRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Unable to write webhook operational log for payment {}: {}", payment.getPaymentId(), ex.getMessage());
        }
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String eventFor(Payment payment) {
        return switch (payment.getStatus()) {
            case SUCCESS -> "payment.success";
            case FAILED -> "payment.failed";
            case CANCELLED -> "payment.cancelled";
            case REQUIRES_ACTION -> "payment.requires_action";
            case PENDING_RECONCILIATION -> "payment.reconciliation";
            default -> "payment.pending";
        };
    }

    private String hmacSha256(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
