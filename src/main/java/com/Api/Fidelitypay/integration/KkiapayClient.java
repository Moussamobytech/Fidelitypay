

package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.KkiapayProperties;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayWaveRequestDTO;
import com.Api.Fidelitypay.model.Agregateur;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.model.Payment;
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
import java.util.Map;

@Component
@Slf4j
public class KkiapayClient implements PayInProviderClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KkiapayProperties kkiapayProperties;
    @org.springframework.beans.factory.annotation.Autowired
    private com.Api.Fidelitypay.repository.AgregateurRepository agregateurRepository;

    public KkiapayClient(RestTemplate restTemplate, KkiapayProperties kkiapayProperties) {
        this.restTemplate = restTemplate;
        this.kkiapayProperties = kkiapayProperties;
    }

    @Override
    public String getProviderName() {
        return "KKIAPAY";
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
        return initiatePayIn(PayInProviderRequest.builder()
                .amount((long) amount)
                .country(country)
                .operator(operator)
                .phone(phone)
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .callbackUrl(kkiapayProperties.getCallbackUrl())
                .flowType("WAVE".equalsIgnoreCase(operator) ? PaymentFlowType.WAVE_REDIRECT : PaymentFlowType.MOBILE_MONEY_REQUEST)
                .build());
    }

    @Override
    public PaymentResult initiatePayIn(PayInProviderRequest request) {
        long start = System.nanoTime();

        try {
<<<<<<< HEAD
            // 🔐 Headers
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	headers.set("x-api-key", resolvePublicKey(request.getCredentials()));
            boolean isWave = request.getFlowType() == PaymentFlowType.WAVE_REDIRECT;
            String endpoint = isWave ? "/api/v1/payments/partner/wave" : "/api/v1/payments/request";

            // 📦 Payload
            KkiapayRequestDTO.KkiapayRequestDTOBuilder payloadBuilder = KkiapayRequestDTO.builder()
                    .amount((int) request.getAmount())
                    .country(request.getCountry())
                    .callback(resolveCallbackUrl(request))
                    .stateData(Map.of("paymentId", request.getPaymentId() != null ? request.getPaymentId() : ""))
                    .partnerId(request.getPaymentId())
                    .reason("FidelityPay payment " + safe(request.getPaymentId()));
=======
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
            String endpoint = isWave ? "/api/v1/payments/partner/wave" : "/api/v1/payments/request";

            // 🔀 Operator Normalization
            String kkiapayOperator = operator != null ? operator.toLowerCase() : "";
            if (kkiapayOperator.contains("mtn")) kkiapayOperator = "momo";
            else if (kkiapayOperator.contains("moov")) kkiapayOperator = "moov";
            else if (kkiapayOperator.contains("orange")) kkiapayOperator = "orange";
            else if (kkiapayOperator.contains("wave")) kkiapayOperator = "wave";
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75

            Object payload;
            if (isWave) {
<<<<<<< HEAD
                payloadBuilder.email(request.getEmail())
                        .name((safe(request.getFirstname()) + " " + safe(request.getLastname())).trim())
                        .success_url(request.getReturnUrl() != null ? request.getReturnUrl() : resolveCallbackUrl(request))
                        .error_url(request.getCancelUrl() != null ? request.getCancelUrl() : resolveCallbackUrl(request));
            } else {
                payloadBuilder.phoneNumber(request.getPhone())
                        .firstname(request.getFirstname())
                        .lastname(request.getLastname());
=======
                // Wave specific payload
                KkiapayWaveRequestDTO wavePayload = new KkiapayWaveRequestDTO();
                wavePayload.setAmount(amount);
                wavePayload.setEmail(email != null && !email.isEmpty() ? email : "customer@example.com");
                wavePayload.setCountry(country != null ? country.toUpperCase() : "SN");
                wavePayload.setName((firstname != null ? firstname : "Client") + " " + (lastname != null ? lastname : "Fidelity"));
                wavePayload.setCallback(kkiapayProperties.getCallbackUrl());
                wavePayload.setReason("Payment via " + operator + " (" + country + ")");
                wavePayload.setSuccess_url(kkiapayProperties.getCallbackUrl());
                wavePayload.setError_url(kkiapayProperties.getCallbackUrl());
                payload = wavePayload;
            } else {
                // Standard Mobile Money payload
                payload = KkiapayRequestDTO.builder()
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
                        .payment_method(kkiapayOperator)
                        .build();
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
            }

            log.info("Kkiapay Final Payload: {}", objectMapper.writeValueAsString(payload));

            HttpEntity<Object> entity = new HttpEntity<>(payload, headers);

            // 📡 Call API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    entity,
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);
<<<<<<< HEAD

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
                result.setProviderTransactionCreated(kkiapayResponse.getTransactionId() != null);
                if (request.getFlowType() == PaymentFlowType.ORANGE_CI_OTP) {
                    result.setRequiresAction(true);
                    result.setNextActionType("SUBMIT_OTP");
                }

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

=======
            result.setSuccess(true);
            
            log.info("Kkiapay SUCCESS | body={}", response.getBody());
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
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
<<<<<<< HEAD
            result.setErrorType(determineErrorType(e));
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                result.setProviderTransactionCreated(true);
            }

=======
            result.setErrorType(ErrorType.INTERNAL_ERROR);
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
            return result;
        }
    }

<<<<<<< HEAD
    @Override
    public PaymentResult validateAction(Payment payment, String actionType, String value, ProviderCredentials credentials) {
        if (!"SUBMIT_OTP".equalsIgnoreCase(actionType)) {
            return PayInProviderClient.super.validateAction(payment, actionType, value);
        }
        long start = System.nanoTime();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", resolvePublicKey(credentials));
            Map<String, Object> payload = Map.of(
                    "transactionId", payment.getProviderPaymentId(),
                    "otp", value,
                    "country", payment.getCountry());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    kkiapayProperties.getApi().getBaseUrl() + "/api/v1/payments/orange-ci/validate",
                    new HttpEntity<>(payload, headers),
                    String.class);
            KkiapayResponseDTO body = response.getBody() != null
                    ? objectMapper.readValue(response.getBody(), KkiapayResponseDTO.class)
                    : null;
            PaymentResult result = new PaymentResult(response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && !"failed".equalsIgnoreCase(body.getStatus()));
            result.setProviderTransactionCreated(true);
            result.setProviderId(payment.getProviderPaymentId());
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs((System.nanoTime() - start) / 1_000_000.0);
            return result;
        } catch (Exception e) {
            PaymentResult result = new PaymentResult(false);
            result.setProviderTransactionCreated(true);
            result.setProviderId(payment.getProviderPaymentId());
            result.setRawResponse(e.getMessage());
            result.setErrorType(determineErrorType(e));
            result.setResponseTimeMs((System.nanoTime() - start) / 1_000_000.0);
            return result;
        }
    }

    @Override
    public PaymentStatus checkStatus(String providerPaymentId, ProviderCredentials credentials) {
        KkiapayResponseDTO status = checkTransactionStatus(providerPaymentId, credentials);
        if (status == null || status.getStatus() == null) {
            return PaymentStatus.PENDING_RECONCILIATION;
        }
        return switch (status.getStatus().toUpperCase()) {
            case "SUCCESS", "SUCCEEDED", "COMPLETED" -> PaymentStatus.SUCCESS;
            case "FAILED" -> PaymentStatus.FAILED;
            case "CANCELLED", "CANCELED" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
=======
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
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
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
        return checkTransactionStatus(transactionId, null);
    }

    public KkiapayResponseDTO checkTransactionStatus(String transactionId, ProviderCredentials credentials) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", resolvePublicKey(credentials));

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

<<<<<<< HEAD
    // Temporary development bridge: if merchant credentials are missing, fall back to
    // application-level keys. Disable payment.providers.allow-global-credentials-fallback
    // outside local/dev and remove this fallback once merchant onboarding is complete.
    private String resolvePublicKey(ProviderCredentials credentials) {
        return credentials != null && credentials.publicKey() != null
                ? credentials.publicKey()
                : kkiapayProperties.getApi().getPublicKey();
    }

    private String resolveCallbackUrl(PayInProviderRequest request) {
        return request.getCallbackUrl() != null ? request.getCallbackUrl() : kkiapayProperties.getCallbackUrl();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
=======
    private String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "...." + key.substring(key.length() - 4);
    }
}
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
