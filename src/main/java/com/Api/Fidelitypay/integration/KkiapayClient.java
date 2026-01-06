package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class KkiapayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kkiapay.api.base-url}")
    private String baseUrl;

    @Value("${kkiapay.api.public-key}")
    private String publicKey;

    @Value("${kkiapay.api.private-key}")
    private String privateKey;

    @Value("${kkiapay.api.secret-key}")
    private String secretKey;

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
    public PaymentResult initiatePayment(double amount, String country, String operator, String phone) {
        long start = System.nanoTime();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-PRIVATE-KEY", privateKey);
            headers.set("X-API-KEY", publicKey);
            headers.set("X-SECRET-KEY", secretKey);

            KkiapayRequestDTO requestDTO = KkiapayRequestDTO.builder()
                    .amount((int) amount)
                    .callback("http://localhost:8080/callback")
                    .phone(phone)
                    .reason("Payment for " + operator)
                    .firstname("John")
                    .lastname("Doe")
                    .build();

            HttpEntity<KkiapayRequestDTO> entity = new HttpEntity<>(requestDTO, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/v1/transactions",
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            // Vérification de succès technique ET logique

            // Verify content type to avoid HTML pages (dashboard login) being interpreted
            // as success
            MediaType contentType = response.getHeaders().getContentType();
            boolean isHtml = contentType != null && contentType.isCompatibleWith(MediaType.TEXT_HTML);

            boolean httpSuccess = response.getStatusCode().is2xxSuccessful();

            // Parse response
            KkiapayResponseDTO kkiapayResponse = null;
            try {
                if (response.getBody() != null && !isHtml) {
                    kkiapayResponse = objectMapper.readValue(response.getBody(), KkiapayResponseDTO.class);
                }
            } catch (Exception e) {
                log.warn("Could not parse Kkiapay JSON response");
            }

            // Logic success check
            boolean logicSuccess = httpSuccess && !isHtml && kkiapayResponse != null
                    && (kkiapayResponse.getStatus() == null || !"FAILED".equalsIgnoreCase(kkiapayResponse.getStatus()));

            result.setSuccess(logicSuccess);

            if (result.isSuccess()) {
                if (kkiapayResponse != null) {
                    result.setProviderId(kkiapayResponse.getTransactionId());
                    result.setPaymentUrl(kkiapayResponse.getUrl());
                }
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
