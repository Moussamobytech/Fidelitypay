package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.KkiapayProperties;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.enums.ErrorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.HttpClientErrorException;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Component
@Slf4j
public class KkiapayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KkiapayProperties kkiapayProperties;
    @org.springframework.beans.factory.annotation.Autowired
    private com.Api.Fidelitypay.repository.AgregateurRepository agregateurRepository;

    public KkiapayClient(RestTemplate restTemplate, KkiapayProperties kkiapayProperties) {
        this.restTemplate = restTemplate;
        this.kkiapayProperties = kkiapayProperties;
    }

    /** Kkiapay availability check */
    public boolean isAvailable() {
        try {
            Agregateur dbConfig = agregateurRepository.findByNomAIgnoreCase("KKIAPAY").orElse(null);
            if (dbConfig == null) return false;
            
            String baseUrl = (dbConfig.getBaseUrl() != null && !dbConfig.getBaseUrl().isEmpty()) 
                             ? dbConfig.getBaseUrl() : "https://api.kkiapay.me";
                             
            restTemplate.getForEntity(baseUrl, String.class);
            return true;
        } catch (HttpStatusCodeException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public PaymentResult initiatePayment(double amount, String country, String operator, String phone, String firstname,
            String lastname, String email) {
        long start = System.nanoTime();

        try {
            // 🔐 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 🔐 STRICT DASHBOARD CONFIGURATION ONLY
            Agregateur dbConfig = agregateurRepository.findByNomAIgnoreCase("KKIAPAY")
                    .orElseThrow(() -> new RuntimeException("KKIAPAY is not configured in the dashboard. Please add it first."));

            String publicKey = dbConfig.getCleApblic();
            String privateKey = dbConfig.getCleApr();
            String secretKey = dbConfig.getCleAmaster();
            String baseUrl = dbConfig.getBaseUrl();

            if (publicKey == null || privateKey == null) {
                throw new RuntimeException("KKIAPAY keys are missing in the dashboard configuration.");
            }

            // Fallback to default base URL only if not specified in dashboard
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://api.kkiapay.me";
            }

            headers.set("x-api-key", publicKey);
            headers.set("x-private-key", privateKey);
            headers.set("x-secret-key", secretKey);

            log.info("Using Kkiapay Keys | PUBLIC='{}' | BASE_URL='{}'", publicKey, baseUrl);
            boolean isWave = "WAVE".equalsIgnoreCase(operator);
            // Use the universal endpoint for better compatibility and to avoid "token required" errors on partner endpoints
            String endpoint = "/api/v1/payments/request";

            String kkiapayOperator = operator.toLowerCase();
            if (kkiapayOperator.contains("mtn")) kkiapayOperator = "momo";
            else if (kkiapayOperator.contains("moov")) kkiapayOperator = "moov";
            
            // 📦 Payload & Phone Sanitization
            String cleanPhone = (phone != null) ? phone.replaceAll("[^0-9]", "") : "";
            
            KkiapayRequestDTO.KkiapayRequestDTOBuilder payloadBuilder = KkiapayRequestDTO.builder()
                    .amount((int) amount)
                    .country(country)
                    .callback(kkiapayProperties.getCallbackUrl())
                    .reason("Payment via " + operator + " (" + country + ")")
                    .operator(kkiapayOperator)
                    .payment_method(kkiapayOperator)
                    .directMethod(kkiapayOperator)
                    .phoneNumber(cleanPhone)
                    .firstname(firstname != null ? firstname : "Client")
                    .lastname(lastname != null ? lastname : "Fidelity");

            if (isWave) {
                payloadBuilder.email(email != null ? email : "customer@example.com")
                        .success_url(kkiapayProperties.getCallbackUrl())
                        .error_url(kkiapayProperties.getCallbackUrl());
            }

            KkiapayRequestDTO finalPayload = payloadBuilder.build();
            try {
                String jsonPayload = objectMapper.writeValueAsString(finalPayload);
                log.info("Sending payload to Kkiapay: {}", jsonPayload);
            } catch (Exception e) {
                log.warn("Could not serialize payload for logging", e);
            }

            HttpEntity<KkiapayRequestDTO> entity = new HttpEntity<>(finalPayload, headers);

            // 📡 Call API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            // 🧠 Parse JSON
            String body = response.getBody();
            KkiapayResponseDTO kkiapayResponse = (body != null)
                    ? objectMapper.readValue(body, KkiapayResponseDTO.class)
                    : null;

            boolean success = response.getStatusCode().is2xxSuccessful()
                    && kkiapayResponse != null
                    && !"FAILED".equalsIgnoreCase(kkiapayResponse.getStatus());

            result.setSuccess(success);

            if (success && kkiapayResponse != null) {
                result.setProviderId(kkiapayResponse.getTransactionId());
                result.setPaymentUrl(isWave ? kkiapayResponse.getWave_launch_url() : kkiapayResponse.getUrl());
                
                // Usually direct API requests are pending user confirmation on their phone
                if ("PENDING".equalsIgnoreCase(kkiapayResponse.getStatus()) || "WAITING".equalsIgnoreCase(kkiapayResponse.getStatus())) {
                    result.setPending(true);
                } else if (!isWave && kkiapayResponse.getStatus() == null) {
                    // For Kkiapay request endpoint, if no status is explicitly returned but it's 200 OK, it's usually pending USSD push
                    result.setPending(true);
                }
                
                // Try to extract actual operator from response if available (might be nested depending on their API)
                // Assuming it might be returned in a future update or via raw response
                // result.setActualOperator(kkiapayResponse.getProviderCommonName());

                log.info("Kkiapay SUCCESS | txId={} | timeMs={}",
                        kkiapayResponse.getTransactionId(), elapsedMs);
            } else {
                String status = (kkiapayResponse != null) ? kkiapayResponse.getStatus() : "UNKNOWN";
                log.warn("Kkiapay FAILED | status={} | body={}", status, response.getBody());

                // Set error type based on HTTP status
                if (response.getStatusCode().value() == 401) {
                    result.setErrorType(ErrorType.AUTHENTICATION);
                } else if (response.getStatusCode().value() == 400) {
                    result.setErrorType(ErrorType.BAD_REQUEST);
                } else if (response.getStatusCode().is5xxServerError()) {
                    result.setErrorType(ErrorType.PROVIDER_DOWN);
                } else {
                    result.setErrorType(ErrorType.UNKNOWN);
                }
            }

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("Kkiapay ERROR | timeMs={}", elapsedMs, e);

            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());
            result.setErrorType(determineErrorType(e));

            return result;
        }
    }

    private ErrorType determineErrorType(Exception e) {
        if (e instanceof SocketTimeoutException
                || e.getCause() instanceof SocketTimeoutException) {
            return ErrorType.TIMEOUT;
        } else if (e instanceof UnknownHostException
                || e.getCause() instanceof UnknownHostException) {
            return ErrorType.NETWORK;
        } else if (e instanceof HttpClientErrorException.Unauthorized) {
            return ErrorType.AUTHENTICATION;
        } else if (e.getMessage() != null && (e.getMessage().toLowerCase().contains("401")
                || e.getMessage().toLowerCase().contains("unauthorized"))) {
            return ErrorType.AUTHENTICATION;
        } else if (e.getMessage() != null && (e.getMessage().contains("500") || e.getMessage().contains("503"))) {
            return ErrorType.PROVIDER_DOWN;
        } else if (e.getMessage() != null && e.getMessage().contains("400")) {
            return ErrorType.BAD_REQUEST;
        } else {
            return ErrorType.UNKNOWN;
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