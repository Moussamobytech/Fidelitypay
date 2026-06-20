package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.Webhook;
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

    public WebhookService(RestTemplate restTemplate, DeveloperWebhookService developerWebhookService,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.developerWebhookService = developerWebhookService;
        this.objectMapper = objectMapper;
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
                .errorType(payment.getErrorType())
                .failureStage(payment.getFailureStage())
                .updatedAt(payment.getUpdatedAt())
                .build();

        if (payment.getUser() != null) {
            List<Webhook> webhooks = developerWebhookService.getActiveWebhooksForEvent(payment.getUser().getId(), event);
            for (Webhook webhook : webhooks) {
                sendDeveloperWebhook(webhook, payload, payment.getPaymentId());
            }
            if (!webhooks.isEmpty()) {
                return;
            }
        }

         log.warn("No developer webhook configured for event {}, skipping payment {}", event, payment.getPaymentId());
    }

    private void sendDeveloperWebhook(Webhook webhook, WebhookDTO payload, String paymentId) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-FidelityPay-Event", payload.getEvent());
            headers.set("X-FidelityPay-Signature", "sha256=" + hmacSha256(webhook.getSecret(), body));
            ResponseEntity<Void> response = restTemplate.postForEntity(webhook.getUrl(), new HttpEntity<>(body, headers), Void.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            developerWebhookService.updateWebhookTrigger(webhook.getId(), response.getStatusCode().value(), success);
            log.info("Developer webhook {} sent for payment {}", webhook.getId(), paymentId);
        } catch (Exception ex) {
            developerWebhookService.updateWebhookTrigger(webhook.getId(), 0, false);
            log.error("Developer webhook {} failed for payment {}: {}", webhook.getId(), paymentId, ex.getMessage());
        }
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
