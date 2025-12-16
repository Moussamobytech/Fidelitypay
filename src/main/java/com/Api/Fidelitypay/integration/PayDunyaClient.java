package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaInvoice;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Objects;

@Component
@Slf4j
public class PayDunyaClient {

    private final RestTemplate restTemplate;

    @Value("${paydunya.api.base-url}")
    private String baseUrl;

    @Value("${paydunya.api.public-key}")
    private String publicKey;

    @Value("${paydunya.api.private-key}")
    private String privateKey;

    @Value("${paydunya.api.token}")
    private String masterToken;

    @Value("${paydunya.store.name}")
    private String storeName;

    public PayDunyaClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Vérifie si PayDunya est disponible
     */
    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/health",
                    String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("PayDunya unavailable: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Initie un paiement PayDunya
     */
    public boolean initiatePayment(double amount, String country, String operator) {

        try {
            // 1️⃣ Headers de sécurité PayDunya
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("PAYDUNYA-MASTER-KEY", masterToken);
            headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
            headers.set("PAYDUNYA-PUBLIC-KEY", publicKey);

            // 2️⃣ Payload
            PayDunyaInvoice invoice = new PayDunyaInvoice(
                    amount,
                    "Payment via " + operator + " (" + country + ")");

            PayDunyaRequest request = new PayDunyaRequest(invoice, storeName);

            HttpEntity<PayDunyaRequest> entity = new HttpEntity<>(request, headers);

            // 3️⃣ API Call
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/checkout-invoice",
                    Objects.requireNonNull(HttpMethod.POST),
                    entity,
                    String.class);

            // 4️⃣ Validation réponse
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("PayDunya payment initiated successfully | Amount={}", amount);
                return true;
            }

            log.warn("PayDunya payment failed | Status={}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            log.error("PayDunya payment error", e);
            return false;
        }
    }
}
