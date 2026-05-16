

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
            String baseUrl = (dbConfig != null && dbConfig.getBaseUrl() != null && !dbConfig.getBaseUrl().isEmpty()) 
                             ? dbConfig.getBaseUrl() : "https://api.kkiapay.me";

            restTemplate.getForEntity(baseUrl, String.class);
            return true;
        } catch (HttpStatusCodeException e) {
            // 4xx or 5xx means server responded, so it's up
            return true;
        } catch (Exception e) {
            log.error("Kkiapay availability check failed", e);
            return false;
        }
    }

    public PaymentResult initiatePayment(double amount, String country, String operator, String phone, String firstname,
            String lastname, String email) {
        long start = System.nanoTime();

        try {
            // 🔐 STRICT DASHBOARD CONFIGURATION ONLY
            Agregateur dbConfig = agregateurRepository.findByNomAIgnoreCase("KKIAPAY")
                    .orElseThrow(() -> new RuntimeException("KKIAPAY is not configured in the dashboard. Please add it first."));

            String publicKey = dbConfig.getCleApblic();
            String privateKey = dbConfig.getCleApr();
            String baseUrl = dbConfig.getBaseUrl();

            if (publicKey == null || privateKey == null) {
                throw new RuntimeException("KKIAPAY Public or Private keys are missing in the dashboard configuration.");
            }

            // Fallback to default base URL only if not specified in dashboard
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://api.kkiapay.me";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", publicKey);
            headers.set("x-private-key", privateKey);

            log.info("Kkiapay Attempt | URL: {} | Public: {}...", baseUrl, mask(publicKey));

            boolean isWave = "WAVE".equalsIgnoreCase(operator);
            // On unifie l'URL car l'endpoint /partner/wave semble exiger un token widget.
            // L'endpoint /request est plus flexible pour le débit direct.
            String endpoint = "/api/v1/payments/request";

            // 🔀 Operator Normalization
            String kkiapayOperator = operator != null ? operator.toLowerCase() : "";
            if (kkiapayOperator.contains("mtn")) kkiapayOperator = "momo";
            else if (kkiapayOperator.contains("moov")) kkiapayOperator = "moov";
            else if (kkiapayOperator.contains("orange")) kkiapayOperator = "orange";
            else if (kkiapayOperator.contains("wave")) kkiapayOperator = "wave";

            // 📦 Payload following strict documentation
            KkiapayRequestDTO.KkiapayRequestDTOBuilder payloadBuilder = KkiapayRequestDTO.builder()
                    .amount(amount)
                    .phoneNumber(formatPhoneNumber(phone, country))
                    .country(country)
                    .firstname(firstname != null && !firstname.isEmpty() ? firstname : "Client")
                    .lastname(lastname != null && !lastname.isEmpty() ? lastname : "Fidelity")
                    .callback(kkiapayProperties.getCallbackUrl())
                    .reason("Payment via " + operator + " (" + country + ")")
                    .email(email != null && !email.isEmpty() ? email : "customer@example.com")
                    .name((firstname != null ? firstname : "Client") + " " + (lastname != null ? lastname : "Fidelity"))
                    .operator(kkiapayOperator)
                    .payment_method(kkiapayOperator);

            if (isWave) {
                payloadBuilder.success_url(kkiapayProperties.getCallbackUrl())
                              .error_url(kkiapayProperties.getCallbackUrl());
            }

            KkiapayRequestDTO payload = payloadBuilder.build();
            log.info("Kkiapay Final Payload: {}", objectMapper.writeValueAsString(payload));

            HttpEntity<KkiapayRequestDTO> entity = new HttpEntity<>(payload, headers);

            // 📡 Call API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);
            result.setSuccess(true);
            
            log.info("Kkiapay SUCCESS | body={}", response.getBody());
            return result;

        } catch (HttpStatusCodeException e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("Kkiapay API ERROR | Status: {} | Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse("Status: " + e.getStatusCode() + " | Body: " + e.getResponseBodyAsString());
            result.setErrorType(determineErrorType(e));
            return result;
        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("Kkiapay SYSTEM ERROR", e);
            PaymentResult result = new PaymentResult(false);
            result.setResponseTimeMs(elapsedMs);
            result.setRawResponse(e.getMessage());
            result.setErrorType(ErrorType.UNKNOWN);
            return result;
        }
    }

    private String formatPhoneNumber(String phone, String country) {
        if (phone == null || phone.isEmpty()) return "";
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        
        String prefix = "";
        if ("BJ".equalsIgnoreCase(country)) prefix = "229";
        else if ("CI".equalsIgnoreCase(country)) prefix = "225";
        else if ("SN".equalsIgnoreCase(country)) prefix = "221";
        else if ("TG".equalsIgnoreCase(country)) prefix = "228";
        else if ("ML".equalsIgnoreCase(country)) prefix = "223";

        if (!prefix.isEmpty() && !cleanPhone.startsWith(prefix)) {
            if (cleanPhone.startsWith("0")) cleanPhone = cleanPhone.substring(1);
            return prefix + cleanPhone;
        }
        return cleanPhone;
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

    private String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "...." + key.substring(key.length() - 4);
    }
}