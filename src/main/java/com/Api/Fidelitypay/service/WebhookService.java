package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;

    /**
     * URL du webhook (optionnelle)
     * → ne casse pas le démarrage si absente
     */
    @Value("${webhook.url:}")
    private String webhookUrl;

    public WebhookService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Envoie le paiement au webhook configuré
     */
    public void sendWebhook(Payment payment) {

        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn(
                "Webhook URL not configured, skipping webhook for payment {}",
                payment.getPaymentId()
            );
            return;
        }

        try {
            restTemplate.postForEntity(webhookUrl, payment, Void.class);
            log.info(
                "Webhook sent successfully for payment {}",
                payment.getPaymentId()
            );
        } catch (Exception ex) {
            log.error(
                "Error sending webhook for payment {}",
                payment.getPaymentId(),
                ex
            );
        }
    }
}
