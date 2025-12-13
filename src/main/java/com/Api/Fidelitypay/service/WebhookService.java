package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService {

    @Autowired
    private RestTemplate restTemplate;

    public void sendWebhook(Payment payment) {
        // Envoyer à un endpoint configuré (ex: app cliente)
        String webhookUrl = "https://example.com/webhook"; // Configurable via properties
        restTemplate.postForEntity(webhookUrl, payment, String.class);
    }
}