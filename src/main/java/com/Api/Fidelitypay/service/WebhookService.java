package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.service.dto.WebhookDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;
    private final com.Api.Fidelitypay.config.WebhookProperties webhookProperties;

    public WebhookService(RestTemplate restTemplate, com.Api.Fidelitypay.config.WebhookProperties webhookProperties) {
        this.restTemplate = restTemplate;
        this.webhookProperties = webhookProperties;
    }

    /**
     * Envoie le paiement au webhook configuré de manière asynchrone
     */
    @Async
    public void sendWebhook(Payment payment) {
        String webhookUrl = webhookProperties.getUrl();

        if (webhookUrl == null || webhookUrl.isBlank() || "http://example.com/webhook".equals(webhookUrl)) {
            log.warn("Webhook URL not configured or remains default, skipping webhook for payment {}",
                    payment.getPaymentId());
            return;
        }

        try {
            WebhookDTO payload = WebhookDTO.builder()
                    .paymentId(payment.getPaymentId())
                    .status(payment.getStatus())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .operator(payment.getOperator())
                    .country(payment.getCountry())
                    .providerPaymentId(payment.getProviderPaymentId())
                    .failureReason(payment.getFailureReason())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            restTemplate.postForEntity(webhookUrl, payload, Void.class);
            log.info("✅ Webhook sent successfully for payment {} with status {}", payment.getPaymentId(),
                    payment.getStatus());
        } catch (Exception ex) {
            log.error("❌ Error sending webhook for payment {}: {}", payment.getPaymentId(), ex.getMessage());
        }
    }
}
