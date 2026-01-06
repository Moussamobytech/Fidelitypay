package com.Api.Fidelitypay.integration;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PayDunyaClient {

    private final RestTemplate restTemplate;

    @Value("${paydunya.api.base-url}")
    private String baseUrl; // https://app.paydunya.com/api/v1

    @Value("${paydunya.api.master-key}")
    private String masterKey;

    @Value("${paydunya.api.private-key}")
    private String privateKey;

    @Value("${paydunya.api.token}")
    private String token;

    @Value("${paydunya.store.name}")
    private String storeName;

    public PayDunyaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** PayDunya ne fournit pas de health check */
    public boolean isAvailable() {
        return true; // éviter de désactiver les routes
    }

    public PaymentResult initiatePayment(double amount, String country, String operator) {
        long start = System.nanoTime();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("PAYDUNYA-MASTER-KEY", masterKey);
            headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
            headers.set("PAYDUNYA-TOKEN", token);

            Map<String, Object> body = Map.of(
                    "invoice", Map.of(
                            "total_amount", amount,
                            "description", "Payment via " + operator + " (" + country + ")"),
                    "store", Map.of(
                            "name", storeName));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/checkout-invoice/create",
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            // PayDunya renvoie HTTP 200 même en cas d'erreur logique (ex: mauvaise clé).
            // On vérifie donc si le JSON contient le code de succès "00".
            boolean httpSuccess = response.getStatusCode().is2xxSuccessful();
            String responseBody = response.getBody();
            boolean logicSuccess = responseBody != null && responseBody.contains("\"response_code\":\"00\"");

            result.setSuccess(httpSuccess && logicSuccess);

            if (result.isSuccess()) {
                log.info("PayDunya success | timeMs={}", elapsedMs);
            } else {
                log.error("PayDunya failed | logicSuccess={} | response={}", logicSuccess, response.getBody());
            }

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("PayDunya error | timeMs={}", elapsedMs, e);

            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());
            return result;
        }
    }
}
