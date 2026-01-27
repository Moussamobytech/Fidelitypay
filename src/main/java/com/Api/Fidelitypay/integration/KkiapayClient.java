package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class KkiapayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final com.Api.Fidelitypay.config.KkiapayProperties kkiapayProperties;

    public KkiapayClient(RestTemplate restTemplate, com.Api.Fidelitypay.config.KkiapayProperties kkiapayProperties) {
        this.restTemplate = restTemplate;
        this.kkiapayProperties = kkiapayProperties;
    }

    /** Kkiapay ne fournit pas de vrai health check */
    public boolean isAvailable() {
        String baseUrl = kkiapayProperties.getApi().getBaseUrl();
        if (baseUrl == null) {
            return false;
        }
        try {
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
            headers.set("x-api-key", kkiapayProperties.getApi().getPublicKey());

            boolean isWave = "WAVE".equalsIgnoreCase(operator);
            String endpoint = isWave ? "/api/v1/payments/partner/wave" : "/api/v1/payments/request";

            // 📦 Payload
            KkiapayRequestDTO.KkiapayRequestDTOBuilder payloadBuilder = KkiapayRequestDTO.builder()
                    .amount((int) amount)
                    .country(country)
                    .callback(kkiapayProperties.getCallbackUrl())
                    .reason("Payment via " + operator + " (" + country + ")");

            if (isWave) {
                payloadBuilder.email(email != null ? email : "customer@example.com")
                        .name(firstname + " " + lastname)
                        .success_url(kkiapayProperties.getCallbackUrl()) // Default to callback
                        .error_url(kkiapayProperties.getCallbackUrl()); // Default to callback
            } else {
                payloadBuilder.phoneNumber(phone)
                        .firstname(firstname)
                        .lastname(lastname);
            }

            HttpEntity<KkiapayRequestDTO> entity = new HttpEntity<>(payloadBuilder.build(), headers);

            // 📡 Call API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    kkiapayProperties.getApi().getBaseUrl() + endpoint,
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            // 🧠 Parse JSON
            KkiapayResponseDTO kkiapayResponse = objectMapper.readValue(response.getBody(), KkiapayResponseDTO.class);

            boolean success = response.getStatusCode().is2xxSuccessful()
                    && kkiapayResponse != null
                    && !"FAILED".equalsIgnoreCase(kkiapayResponse.getStatus());

            result.setSuccess(success);

            if (success) {
                result.setProviderId(kkiapayResponse.getTransactionId());
                result.setPaymentUrl(isWave ? kkiapayResponse.getWave_launch_url() : kkiapayResponse.getUrl());

                log.info("Kkiapay SUCCESS | txId={} | timeMs={}",
                        kkiapayResponse.getTransactionId(), elapsedMs);
            } else {
                String status = (kkiapayResponse != null) ? kkiapayResponse.getStatus() : "UNKNOWN";
                log.warn("Kkiapay FAILED | status={} | body={}", status, response.getBody());
            }

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("Kkiapay ERROR | timeMs={}", elapsedMs, e);

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

    public KkiapayResponseDTO checkTransactionStatus(String transactionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", kkiapayProperties.getApi().getPublicKey());

            String payload = "{\"transactionId\":\"" + transactionId + "\"}";
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<KkiapayResponseDTO> response = restTemplate.postForEntity(
                    kkiapayProperties.getApi().getBaseUrl() + "/api/v1/transactions/status",
                    entity,
                    KkiapayResponseDTO.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error checking Kkiapay status for txId={}", transactionId, e);
            return null;
        }
    }
}
