package com.Api.Fidelitypay.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SamirPayClient {

    @Autowired
    private RestTemplate restTemplate;

    private final String baseUrl = "https://api.samirpay.com"; // Exemple

    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    // Méthode pour initiatePayment, etc. (implémenter selon doc API)
}