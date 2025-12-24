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
     * Initie un paiement PayDunya et renvoie un objet décrit la réponse
     */
    public PaymentResult initiatePayment(double amount, String country, String operator) {
        long start = System.nanoTime();
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
            long callStart = System.nanoTime();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/checkout-invoice",
                    Objects.requireNonNull(HttpMethod.POST),
                    entity,
                    String.class);
            long callEnd = System.nanoTime();

            double elapsedMs = (callEnd - callStart) / 1_000_000.0;

            // 4️⃣ Build result
            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);
            result.setSuccess(response.getStatusCode().is2xxSuccessful());

            try {
                String body = response.getBody();
                if (body != null) {
                    if (body.contains("invoice_id")) {
                        result.setProviderId("extracted-invoice-id");
                    }
                    if (body.contains("url")) {
                        result.setPaymentUrl("extracted-url");
                    }
                }
            } catch (Exception ignored) {
            }

            if (result.isSuccess()) {
                log.info("PayDunya payment initiated successfully | Amount={} | timeMs={}", amount, elapsedMs);
            } else {
                log.warn("PayDunya payment failed | Status={} | timeMs={}", response.getStatusCode(), elapsedMs);
            }

            return result;

        } catch (Exception e) {
            long end = System.nanoTime();
            double elapsedMs = (end - start) / 1_000_000.0;
            log.error("PayDunya payment error | timeMs={}", elapsedMs, e);
            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());
            return result;
        }
    }
}
