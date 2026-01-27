package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Payment;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
     * Envoie le paiement au webhook configuré
     */
    public void sendWebhook(Payment payment) {
        String webhookUrl = webhookProperties.getUrl();

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn(
                    "Webhook URL not configured, skipping webhook for payment {}",
                    payment.getPaymentId());
            return;
        }

        try {
            restTemplate.postForEntity(Objects.requireNonNull(webhookUrl), payment, Void.class);
            log.info(
                    "Webhook sent successfully for payment {}",
                    payment.getPaymentId());
        } catch (Exception ex) {
            log.error(
                    "Error sending webhook for payment {}",
                    payment.getPaymentId(),
                    ex);
        }
    }
}
