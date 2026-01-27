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
    private final com.Api.Fidelitypay.config.PaydunyaProperties paydunyaProperties;

    public PayDunyaClient(RestTemplate restTemplate, com.Api.Fidelitypay.config.PaydunyaProperties paydunyaProperties) {
        this.restTemplate = restTemplate;
        this.paydunyaProperties = paydunyaProperties;
    }

    /** PayDunya ne fournit pas de health check */
    public boolean isAvailable() {
        String baseUrl = paydunyaProperties.getApi().getBaseUrl();
        if (baseUrl == null) {
            return false;
        }
        try {
            // Simple ping to base URL to check connectivity
            restTemplate.getForEntity(baseUrl, String.class);
            return true;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // 4xx or 5xx means server responded
            return true;
        }
    }

    public PaymentResult initiatePayment(double amount, String country, String operator, String phone, String firstname,
            String lastname, String email) {
        long start = System.nanoTime();

        try {
            // 🔐 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("PAYDUNYA-MASTER-KEY", paydunyaProperties.getApi().getMasterKey());
            headers.set("PAYDUNYA-PRIVATE-KEY", paydunyaProperties.getApi().getPrivateKey());
            headers.set("PAYDUNYA-TOKEN", paydunyaProperties.getApi().getToken());

            // 📦 Payload
            PayDunyaRequestDTO payload = new PayDunyaRequestDTO(
                    new PayDunyaInvoiceDTO(
                            amount,
                            "Payment via " + operator + " (" + country + ")"),
                    new PayDunyaStoreDTO(paydunyaProperties.getStore().getName()));

            HttpEntity<PayDunyaRequestDTO> entity = new HttpEntity<>(payload, headers);

            // 📡 Call API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    paydunyaProperties.getApi().getBaseUrl() + "/checkout-invoice/create",
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

            if (e instanceof java.net.SocketTimeoutException
                    || e.getCause() instanceof java.net.SocketTimeoutException) {
                result.setErrorType(com.Api.Fidelitypay.Enum.ErrorType.TIMEOUT);
            } else if (e instanceof java.net.UnknownHostException
                    || e.getCause() instanceof java.net.UnknownHostException) {
                result.setErrorType(com.Api.Fidelitypay.Enum.ErrorType.NETWORK);
            } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                result.setErrorType(com.Api.Fidelitypay.Enum.ErrorType.AUTHENTICATION);
            } else if (e.getMessage() != null && (e.getMessage().contains("500") || e.getMessage().contains("503"))) {
                result.setErrorType(com.Api.Fidelitypay.Enum.ErrorType.PROVIDER_DOWN);
            } else {
                result.setErrorType(com.Api.Fidelitypay.Enum.ErrorType.UNKNOWN);
            }

            return result;
        }
    }
}
