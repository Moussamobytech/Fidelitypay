// package com.Api.Fidelitypay.integration;

// import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
// import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestTemplate;

// @Component
// @Slf4j
// public class KkiapayClient {

//     private final RestTemplate restTemplate;
//     private final ObjectMapper objectMapper = new ObjectMapper();
//     private final com.Api.Fidelitypay.config.KkiapayProperties kkiapayProperties;

//     public KkiapayClient(RestTemplate restTemplate, com.Api.Fidelitypay.config.KkiapayProperties kkiapayProperties) {
//         this.restTemplate = restTemplate;
//         this.kkiapayProperties = kkiapayProperties;
//     }

//     /** Kkiapay ne fournit pas de vrai health check */
//     public boolean isAvailable() {
//         String baseUrl = kkiapayProperties.getApi().getBaseUrl();
//         if (baseUrl == null) {
//             return false;
//         }
//         try {
//             restTemplate.getForEntity(baseUrl, String.class);
//             return true;
//         } catch (org.springframework.web.client.HttpStatusCodeException e) {
//             // 4xx or 5xx means server responded
//             return true;
//         }
//     }

//     public PaymentResult initiatePayment(double amount, String country, String operator, String phone, String firstname,
//             String lastname, String email) {
//         long start = System.nanoTime();

//         try {
//             // 🔐 Headers
//             HttpHeaders headers = new HttpHeaders();
//             headers.setContentType(MediaType.APPLICATION_JSON);
//             headers.set("x-api-key", kkiapayProperties.getApi().getPublicKey());

//             boolean isWave = "WAVE".equalsIgnoreCase(operator);
//             String endpoint = isWave ? "/api/v1/payments/partner/wave" : "/api/v1/payments/request";

//             // 📦 Payload
//             KkiapayRequestDTO.KkiapayRequestDTOBuilder payloadBuilder = KkiapayRequestDTO.builder()
//                     .amount((int) amount)
//                     .country(country)
//                     .callback(kkiapayProperties.getCallbackUrl())
//                     .reason("Payment via " + operator + " (" + country + ")");

//             if (isWave) {
//                 payloadBuilder.email(email != null ? email : "customer@example.com")
//                         .name(firstname + " " + lastname)
//                         .success_url(kkiapayProperties.getCallbackUrl()) // Default to callback
//                         .error_url(kkiapayProperties.getCallbackUrl()); // Default to callback
//             } else {
//                 payloadBuilder.phoneNumber(phone)
//                         .firstname(firstname)
//                         .lastname(lastname);
//             }

//             HttpEntity<KkiapayRequestDTO> entity = new HttpEntity<>(payloadBuilder.build(), headers);

//             // 📡 Call API
//             ResponseEntity<String> response = restTemplate.postForEntity(
//                     kkiapayProperties.getApi().getBaseUrl() + endpoint,
//                     entity,
//                     String.class);

//             double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;

//             PaymentResult result = new PaymentResult();
//             result.setRawResponse(response.getBody());
//             result.setResponseTimeMs(elapsedMs);

//             // 🧠 Parse JSON
//             KkiapayResponseDTO kkiapayResponse = objectMapper.readValue(response.getBody(), KkiapayResponseDTO.class);

//             boolean success = response.getStatusCode().is2xxSuccessful()
//                     && kkiapayResponse != null
//                     && !"FAILED".equalsIgnoreCase(kkiapayResponse.getStatus());

//             result.setSuccess(success);

//             if (success) {
//                 result.setProviderId(kkiapayResponse.getTransactionId());
//                 result.setPaymentUrl(isWave ? kkiapayResponse.getWave_launch_url() : kkiapayResponse.getUrl());

//                 log.info("Kkiapay SUCCESS | txId={} | timeMs={}",
//                         kkiapayResponse.getTransactionId(), elapsedMs);
//             } else {
//                 String status = (kkiapayResponse != null) ? kkiapayResponse.getStatus() : "UNKNOWN";
//                 log.warn("Kkiapay FAILED | status={} | body={}", status, response.getBody());
//             }

//             return result;

//         } catch (Exception e) {
//             double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
//             log.error("Kkiapay ERROR | timeMs={}", elapsedMs, e);

//             PaymentResult result = new PaymentResult(false);
//             result.setResponseTimeMs(elapsedMs);
//             result.setRawResponse(e.getMessage());

//             if (e instanceof java.net.SocketTimeoutException
//                     || e.getCause() instanceof java.net.SocketTimeoutException) {
//                 result.setErrorType(com.Api.Fidelitypay.enums.ErrorType.TIMEOUT);
//             } else if (e instanceof java.net.UnknownHostException
//                     || e.getCause() instanceof java.net.UnknownHostException) {
//                 result.setErrorType(com.Api.Fidelitypay.enums.ErrorType.NETWORK);
//             } else if (e.getMessage() != null && e.getMessage().contains("401")) {
//                 result.setErrorType(com.Api.Fidelitypay.enums.ErrorType.AUTHENTICATION);
//             } else if (e.getMessage() != null && (e.getMessage().contains("500") || e.getMessage().contains("503"))) {
//                 result.setErrorType(com.Api.Fidelitypay.enums.ErrorType.PROVIDER_DOWN);
//             } else {
//                 result.setErrorType(com.Api.Fidelitypay.enums.ErrorType.UNKNOWN);
//             }

//             return result;
//         }
//     }

//     public KkiapayResponseDTO checkTransactionStatus(String transactionId) {
//         try {
//             HttpHeaders headers = new HttpHeaders();
//             headers.setContentType(MediaType.APPLICATION_JSON);
//             headers.set("x-api-key", kkiapayProperties.getApi().getPublicKey());

//             String payload = "{\"transactionId\":\"" + transactionId + "\"}";
//             HttpEntity<String> entity = new HttpEntity<>(payload, headers);

//             ResponseEntity<KkiapayResponseDTO> response = restTemplate.postForEntity(
//                     kkiapayProperties.getApi().getBaseUrl() + "/api/v1/transactions/status",
//                     entity,
//                     KkiapayResponseDTO.class);

//             return response.getBody();
//         } catch (Exception e) {
//             log.error("Error checking Kkiapay status for txId={}", transactionId, e);
//             return null;
//         }
//     }
// }


package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
import com.Api.Fidelitypay.enums.ErrorType;
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
    
    @Value("${kkiapay.simulation.auth-error:false}")
    private boolean simulateAuthError;
    
    @Value("${kkiapay.simulation.mode:none}")
    private String simulationMode; // none, auth-error, random-error

    public KkiapayClient(RestTemplate restTemplate, com.Api.Fidelitypay.config.KkiapayProperties kkiapayProperties) {
        this.restTemplate = restTemplate;
        this.kkiapayProperties = kkiapayProperties;
    }

    /** Kkiapay ne fournit pas de vrai health check */
    public boolean isAvailable() {
        // En mode simulation, retourner true pour que le service puisse être sélectionné
        if (!"none".equals(simulationMode)) {
            return true;
        }
        
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

        // Mode simulation d'erreur d'authentification
        if (simulateAuthError || "auth-error".equals(simulationMode)) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.warn("Kkiapay AUTH ERROR SIMULATION | mode={}", simulationMode);
            
            PaymentResult simulated = new PaymentResult(false);
            simulated.setResponseTimeMs(elapsedMs);
            simulated.setErrorType(ErrorType.AUTHENTICATION);
            simulated.setRawResponse("SIMULATED_AUTH_ERROR: Invalid API key (401 Unauthorized) - {\"status\":\"UNAUTHORIZED\",\"message\":\"Invalid x-api-key\"}");
            
            return simulated;
        }

        try {
            // 🔐 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Simulation d'une clé API invalide
            String apiKey = kkiapayProperties.getApi().getPublicKey();
            if ("random-error".equals(simulationMode)) {
                // Pour simuler une erreur d'authentification aléatoire
                if (Math.random() > 0.7) { // 30% de chance d'erreur d'authentification
                    apiKey = "invalid-api-key-" + System.currentTimeMillis();
                }
            }
            
            headers.set("x-api-key", apiKey);

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
                
                // Vérifier si c'est une erreur d'authentification
                if (response.getStatusCodeValue() == 401) {
                    result.setErrorType(ErrorType.AUTHENTICATION);
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
        if (e instanceof java.net.SocketTimeoutException
                || e.getCause() instanceof java.net.SocketTimeoutException) {
            return ErrorType.TIMEOUT;
        } else if (e instanceof java.net.UnknownHostException
                || e.getCause() instanceof java.net.UnknownHostException) {
            return ErrorType.NETWORK;
        } else if (e instanceof org.springframework.web.client.HttpClientErrorException.Unauthorized) {
            return ErrorType.AUTHENTICATION;
        } else if (e.getMessage() != null && e.getMessage().contains("401")) {
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