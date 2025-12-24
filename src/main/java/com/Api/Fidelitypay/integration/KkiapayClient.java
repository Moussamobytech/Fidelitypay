package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;

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
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(baseUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("KKiaPay health check failed: {}", e.getMessage());
            return false;
        }
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

                KkiapayRequest request = new KkiapayRequest();
                request.setAmount(amount);
                request.setCurrency("XOF");
                request.setReason("Payment for " + operator);
                request.setPublicKey(publicKey);
                request.setTransactionId(UUID.randomUUID().toString());
                request.setService(operator);


            HttpEntity<KkiapayRequest> entity = new HttpEntity<>(request, headers);

            long callStart = System.nanoTime();
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/v1/payments",
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            long callEnd = System.nanoTime();

            double elapsedMs = (callEnd - callStart) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);
            result.setSuccess(response.getStatusCode().is2xxSuccessful());

            // Extraction simple (à remplacer par Jackson plus tard)
            String body = response.getBody();
            if (body != null) {
                if (body.contains("transactionId")) {
                    result.setProviderId("extracted-transaction-id");
                }
                if (body.contains("paymentUrl")) {
                    result.setPaymentUrl("extracted-payment-url");
                }
            }

            if (result.isSuccess()) {
                log.info("KKiaPay payment initiated | amount={} | timeMs={}", amount, elapsedMs);
            } else {
                log.warn("KKiaPay payment failed | status={} | timeMs={}",
                        response.getStatusCode(), elapsedMs);
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
