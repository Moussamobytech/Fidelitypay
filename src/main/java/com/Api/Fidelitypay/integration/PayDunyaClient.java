package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.PaydunyaProperties;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.model.Agregateur;
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
    @org.springframework.beans.factory.annotation.Autowired
    private com.Api.Fidelitypay.repository.AgregateurRepository agregateurRepository;

    public PayDunyaClient(RestTemplate restTemplate, PaydunyaProperties paydunyaProperties) {
        this.restTemplate = restTemplate;
        this.paydunyaProperties = paydunyaProperties;
    }

    /** PayDunya ne fournit pas de health check */
    /** PayDunya availability check */
    public boolean isAvailable() {
        try {
            Agregateur dbConfig = agregateurRepository.findByNomAIgnoreCase("PAYDUNYA").orElse(null);
            if (dbConfig == null) return false;

            String baseUrl = (dbConfig.getBaseUrl() != null && !dbConfig.getBaseUrl().isEmpty()) 
                             ? dbConfig.getBaseUrl() : "https://app.paydunya.com";

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
            // 🔐 STRICT DASHBOARD CONFIGURATION ONLY
            Agregateur dbConfig = agregateurRepository.findByNomAIgnoreCase("PAYDUNYA")
                    .orElseThrow(() -> new RuntimeException("PAYDUNYA is not configured in the dashboard. Please add it first."));

            String masterKey = dbConfig.getCleAmaster();
            String privateKey = dbConfig.getCleApr();
            String token = dbConfig.getCleAtoken();
            String baseUrl = dbConfig.getBaseUrl();

            if (masterKey == null || privateKey == null || token == null) {
                throw new RuntimeException("PAYDUNYA keys or token are missing in the dashboard configuration.");
            }

            // Fallback to default base URL only if not specified in dashboard
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://app.paydunya.com";
            }

            // 🧐 Debug Log (Masked)
            log.info("PayDunya Attempt | URL: {} | MasterKey: {}... | PrivateKey: {}... | Token: {}...",
                    baseUrl, mask(masterKey), mask(privateKey), mask(token));

            // 🔐 Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("PAYDUNYA-MASTER-KEY", masterKey);
            headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
            headers.set("PAYDUNYA-TOKEN", token);

            // 📦 Payload
            PayDunyaInvoiceDTO invoiceDTO = new PayDunyaInvoiceDTO(
                    amount,
                    "Payment via " + operator + " (" + country + ")",
                    getPayDunyaChannels(operator, country));

            PayDunyaRequestDTO payload = new PayDunyaRequestDTO(
                    invoiceDTO,
                    new PayDunyaStoreDTO(paydunyaProperties.getStore().getName()));

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
                
                // Map logical error codes to ErrorTypes for fallback
                String code = payDunyaResponse.getResponseCode();
                if ("1001".equals(code) || "1002".equals(code) || "4001".equals(code)) {
                    result.setErrorType(ErrorType.AUTHENTICATION);
                } else if ("400".equals(code) || "1003".equals(code)) {
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

    private java.util.List<String> getPayDunyaChannels(String operator, String country) {
        if (operator == null || country == null) return java.util.List.of("unknown");
        
        String op = operator.toLowerCase().trim();
        String countryCode = country.toLowerCase().trim();
        
        // Basic normalization for common operator names
        if (op.contains("orange") || op.equals("om")) op = "orange-money";
        if (op.contains("free")) op = "free-money";
        if (op.contains("tresor")) op = "tresormoney";

        // Generate slug using the country code from dashboard (e.g., wave-sn, orange-money-ci)
        // Note: PayDunya sometimes expects full names for Senegal (senegal), 
        // but we'll try to use the code provided in the dashboard for maximum flexibility.
        String slug = op + "-" + countryCode;
        
        // Special case: if country is SN, many providers expect 'senegal'
        if (countryCode.equals("sn")) slug = op + "-senegal";
        
        log.info("Dynamically generated PayDunya channel: {}", slug);
        return java.util.List.of(slug);
    }
    
    private String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        if (key.contains("*")) return key; // Already a placeholder or asterisk
        return key.substring(0, 4) + "...." + key.substring(key.length() - 4);
    }
}
