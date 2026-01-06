package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.integration.paydunya.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class PayDunyaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${paydunya.api.base-url}")
    private String baseUrl;

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
        return true;
    }

    public PaymentResult initiatePayment(double amount, String country, String operator, String phone) {
        long start = System.nanoTime();

        try {
            // 🔐 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("PAYDUNYA-MASTER-KEY", masterKey);
            headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
            headers.set("PAYDUNYA-TOKEN", token);

            // 📦 Payload
            PayDunyaRequestDTO payload = new PayDunyaRequestDTO(
                    new PayDunyaInvoiceDTO(
                            amount,
                            "Payment via " + operator + " (" + country + ")"),
                    new PayDunyaStoreDTO(storeName));

            HttpEntity<PayDunyaRequestDTO> entity = new HttpEntity<>(payload, headers);

            // 📡 Call API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/checkout-invoice/create",
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            // 🧠 Parse JSON
            PayDunyaResponseDTO payDunyaResponse = objectMapper.readValue(response.getBody(),
                    PayDunyaResponseDTO.class);

            boolean success = "00".equals(payDunyaResponse.getResponseCode());
            result.setSuccess(success);

            if (success) {
                result.setProviderId(payDunyaResponse.getToken());
                result.setPaymentUrl(payDunyaResponse.getResponseText());

                log.info("PayDunya SUCCESS | token={} | timeMs={}",
                        payDunyaResponse.getToken(), elapsedMs);
            } else {
                log.warn("PayDunya FAILED | code={} | msg={}",
                        payDunyaResponse.getResponseCode(),
                        payDunyaResponse.getDescription());
            }

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("PayDunya ERROR | timeMs={}", elapsedMs, e);

            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());
            return result;
        }
    }
}
