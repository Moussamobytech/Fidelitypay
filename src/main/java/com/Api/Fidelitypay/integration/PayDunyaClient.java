package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.PaydunyaProperties;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.integration.paydunya.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Component
@Slf4j
public class PayDunyaClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaydunyaProperties paydunyaProperties;

    public PayDunyaClient(RestTemplate restTemplate, PaydunyaProperties paydunyaProperties) {
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
        } catch (HttpStatusCodeException e) {
            // 4xx or 5xx means server responded
            return true;
        } catch (Exception e) {
            log.error("PayDunya availability check failed", e);
            return false;
        }
    }

    public PaymentResult initiatePayment(double amount, String country, String operator, String phone, String firstname,
            String lastname, String email) {
        long start = System.nanoTime();

        try {
            // 🧐 Debug Log (Masked)
            log.info("PayDunya Attempt | URL: {} | MasterKey: {}... | PrivateKey: {}... | Token: {}...",
                    paydunyaProperties.getApi().getBaseUrl(),
                    mask(paydunyaProperties.getApi().getMasterKey()),
                    mask(paydunyaProperties.getApi().getPrivateKey()),
                    mask(paydunyaProperties.getApi().getToken()));

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
                
                // Map logical error codes to ErrorTypes for fallback
                String code = payDunyaResponse.getResponseCode();
                if ("1001".equals(code) || "1002".equals(code) || "4001".equals(code)) {
                    result.setErrorType(ErrorType.AUTHENTICATION);
                } else if ("400".equals(code)) {
                    result.setErrorType(ErrorType.BAD_REQUEST);
                } else {
                    result.setErrorType(ErrorType.UNKNOWN);
                }
            }

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("PayDunya ERROR | timeMs={}", elapsedMs, e);

            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());

            if (e instanceof SocketTimeoutException
                    || e.getCause() instanceof SocketTimeoutException) {
                result.setErrorType(ErrorType.TIMEOUT);
            } else if (e instanceof UnknownHostException
                    || e.getCause() instanceof UnknownHostException) {
                result.setErrorType(ErrorType.NETWORK);
            } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                result.setErrorType(ErrorType.AUTHENTICATION);
            } else if (e.getMessage() != null && (e.getMessage().contains("500") || e.getMessage().contains("503"))) {
                result.setErrorType(ErrorType.PROVIDER_DOWN);
            } else {
                result.setErrorType(ErrorType.UNKNOWN);
            }

            return result;
        }
    }
    
    private String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        if (key.contains("*")) return key; // Already a placeholder or asterisk
        return key.substring(0, 4) + "...." + key.substring(key.length() - 4);
    }
}
