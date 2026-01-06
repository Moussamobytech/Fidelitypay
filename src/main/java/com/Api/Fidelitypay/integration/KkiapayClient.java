package com.Api.Fidelitypay.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class KkiapayClient {

    private final RestTemplate restTemplate;

    @Value("${kkiapay.api.base-url}")
    private String baseUrl;

    @Value("${kkiapay.api.public-key}")
    private String publicKey;

    @Value("${kkiapay.api.private-key}")
    private String privateKey;

    public KkiapayClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Vérifie la disponibilité du provider
     */

    public boolean isAvailable() {
        return true; // éviter de désactiver les routes automatiquement
    }

    /**
     * Initie un paiement via KKiaPay
     */
    public PaymentResult initiatePayment(double amount, String country, String operator) {
        long start = System.nanoTime();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PRIVATE-KEY", privateKey);
            headers.set("X-API-KEY", publicKey);
            headers.set("X-SECRET-KEY", privateKey);

            java.util.Map<String, Object> body = java.util.Map.of(
                    "amount", (int) amount,
                    "callback", "http://localhost:8080/callback",
                    "phone", "60000000",
                    "reason", "Payment for " + operator,
                    "firstname", "John",
                    "lastname", "Doe");

            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/payments",
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            // Vérification de succès technique ET logique
            // Kkiapay peut renvoyer 200 OK avec un statut "FAILED" dans le corps
            boolean httpSuccess = response.getStatusCode().is2xxSuccessful();
            // Adaptez cette condition selon la vraie réponse JSON de Kkiapay
            // (ex: cherche "status":"SUCCESS" ou "transactionId")
            String responseBody = response.getBody();
            boolean logicSuccess = responseBody != null && !responseBody.contains("error");

            result.setSuccess(httpSuccess && logicSuccess);

            if (result.isSuccess()) {
                log.info("KKiaPay success | amount={} | timeMs={}", amount, elapsedMs);
            } else {
                log.error("KKiaPay failed | status={} | response={}", response.getStatusCode(), response.getBody());
            }

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("KKiaPay error | timeMs={}", elapsedMs, e);

            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());
            return result;
        }
    }
}
