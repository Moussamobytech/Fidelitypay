package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.KkiapayProperties;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayWaveRequestDTO;
import com.Api.Fidelitypay.model.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

@Component
@Slf4j
public class KkiapayClient implements PayInProviderClient {

    private static final String SANDBOX_BASE_URL = "https://api-sandbox.kkiapay.me";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KkiapayProperties kkiapayProperties;

    public KkiapayClient(RestTemplate restTemplate, KkiapayProperties kkiapayProperties) {
        this.restTemplate = restTemplate;
        this.kkiapayProperties = kkiapayProperties;
    }

    @Override
    public String getProviderName() {
        return "KKIAPAY";
    }

    public boolean isAvailable() {
        String baseUrl = kkiapayProperties.getApi().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            restTemplate.getForEntity(baseUrl, String.class);
            return true;
        } catch (HttpStatusCodeException e) {
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
        String baseUrl = resolveBaseUrl(request.getCredentials(), request.getEnvironment());
        boolean isWave = request.getFlowType() == PaymentFlowType.WAVE_REDIRECT;
        String endpoint = isWave ? "/api/v1/payments/partner/wave" : "/api/v1/payments/request";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", resolvePublicKey(request.getCredentials()));

            Object payload = isWave ? wavePayload(request) : mobileMoneyPayload(request);
            log.info("Kkiapay Attempt | URL={} | publicKey={} | payload={}",
                    baseUrl, mask(resolvePublicKey(request.getCredentials())), objectMapper.writeValueAsString(payload));

            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + endpoint,
                    new HttpEntity<>(payload, headers),
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            KkiapayResponseDTO body = response.getBody() != null && !response.getBody().isBlank()
                    ? objectMapper.readValue(response.getBody(), KkiapayResponseDTO.class)
                    : null;
            boolean success = response.getStatusCode().is2xxSuccessful()
                    && body != null
                    && !"FAILED".equalsIgnoreCase(body.getStatus());
            result.setSuccess(success);

            if (success) {
                String transactionId = body.resolvedTransactionId();
                result.setProviderId(transactionId);
                result.setPaymentUrl(isWave ? body.getWave_launch_url() : body.getUrl());
                result.setProviderTransactionCreated(transactionId != null);
                if (request.getFlowType() == PaymentFlowType.ORANGE_CI_OTP) {
                    result.setRequiresAction(true);
                    result.setNextActionType("SUBMIT_OTP");
                }
                log.info("Kkiapay SUCCESS | txId={} | timeMs={}", transactionId, elapsedMs);
            } else {
                result.setErrorType(errorTypeForStatus(response.getStatusCode().value()));
                log.warn("Kkiapay FAILED | status={} | body={}", body != null ? body.getStatus() : "UNKNOWN", response.getBody());
            }
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
            result.setErrorType(determineErrorType(e));
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                result.setProviderTransactionCreated(true);
            }
            return result;
        }
    }

    @Override
    public PaymentResult validateAction(Payment payment, String actionType, String value, ProviderCredentials credentials) {
        if (!"SUBMIT_OTP".equalsIgnoreCase(actionType)) {
            return PayInProviderClient.super.validateAction(payment, actionType, value, credentials);
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
                    resolveBaseUrl(credentials) + "/api/v1/payments/orange-ci/validate",
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
            ResponseEntity<KkiapayResponseDTO> response = restTemplate.postForEntity(
                    resolveBaseUrl(credentials) + "/api/v1/transactions/status",
                    new HttpEntity<>(payload, headers),
                    KkiapayResponseDTO.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error checking Kkiapay status for txId={}", transactionId, e);
            return null;
        }
    }

    private KkiapayWaveRequestDTO wavePayload(PayInProviderRequest request) {
        KkiapayWaveRequestDTO payload = new KkiapayWaveRequestDTO();
        payload.setAmount(request.getAmount());
        payload.setEmail(nonBlank(request.getEmail(), "customer@example.com"));
        payload.setCountry(normalizeCountry(request.getCountry(), "SN"));
        payload.setName((nonBlank(request.getFirstname(), "Client") + " " + nonBlank(request.getLastname(), "Fidelity")).trim());
        payload.setCallback(resolveCallbackUrl(request));
        payload.setStateData(Map.of("paymentId", nonBlank(request.getPaymentId(), "")));
        payload.setPartnerId(request.getPaymentId());
        payload.setReason("FidelityPay payment " + nonBlank(request.getPaymentId(), ""));
        payload.setSuccess_url(request.getReturnUrl() != null ? request.getReturnUrl() : resolveCallbackUrl(request));
        payload.setError_url(request.getCancelUrl() != null ? request.getCancelUrl() : resolveCallbackUrl(request));
        return payload;
    }

    private KkiapayRequestDTO mobileMoneyPayload(PayInProviderRequest request) {
        String operator = normalizeOperator(request.getProviderChannel() != null ? request.getProviderChannel() : request.getOperator());
        return KkiapayRequestDTO.builder()
                .amount(request.getAmount())
                .phoneNumber(formatPhoneNumber(request.getPhone(), request.getCountry()))
                .country(normalizeCountry(request.getCountry(), null))
                .firstname(nonBlank(request.getFirstname(), "Client"))
                .lastname(nonBlank(request.getLastname(), "Fidelity"))
                .callback(resolveCallbackUrl(request))
                .stateData(Map.of("paymentId", nonBlank(request.getPaymentId(), "")))
                .partnerId(request.getPaymentId())
                .reason("FidelityPay payment " + nonBlank(request.getPaymentId(), ""))
                .email(nonBlank(request.getEmail(), "customer@example.com"))
                .name((nonBlank(request.getFirstname(), "Client") + " " + nonBlank(request.getLastname(), "Fidelity")).trim())
                .operator(operator)
                .payment_method(operator)
                .build();
    }

    private String formatPhoneNumber(String phone, String country) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        String prefix = switch (normalizeCountry(country, "")) {
            case "BJ" -> "229";
            case "CI" -> "225";
            case "SN" -> "221";
            case "TG" -> "228";
            case "ML" -> "223";
            default -> "";
        };
        if (!prefix.isEmpty() && !cleanPhone.startsWith(prefix)) {
            if (cleanPhone.startsWith("0")) {
                cleanPhone = cleanPhone.substring(1);
            }
            return prefix + cleanPhone;
        }
        return cleanPhone;
    }

    private String normalizeOperator(String value) {
        String operator = value == null ? "" : value.toLowerCase().trim();
        if (operator.contains("mtn")) return "momo";
        if (operator.contains("moov")) return "moov";
        if (operator.contains("orange") || operator.equals("om")) return "orange";
        if (operator.contains("wave")) return "wave";
        return operator;
    }

    private String normalizeCountry(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private ErrorType errorTypeForStatus(int status) {
        if (status == 401 || status == 403) return ErrorType.AUTHENTICATION;
        if (status == 400) return ErrorType.BAD_REQUEST;
        if (status >= 500) return ErrorType.PROVIDER_DOWN;
        return ErrorType.UNKNOWN;
    }

    private ErrorType determineErrorType(Exception e) {
        if (e instanceof SocketTimeoutException || e.getCause() instanceof SocketTimeoutException) {
            return ErrorType.TIMEOUT;
        } else if (e instanceof UnknownHostException || e.getCause() instanceof UnknownHostException) {
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
        }
        return ErrorType.UNKNOWN;
    }

    private String resolveBaseUrl(ProviderCredentials credentials, String environment) {
        String configured = credentials == null ? null : credentials.get("baseUrl");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return "SANDBOX".equalsIgnoreCase(environment)
                ? SANDBOX_BASE_URL
                : kkiapayProperties.getApi().getBaseUrl();
    }

    private String resolveBaseUrl(ProviderCredentials credentials) {
        return resolveBaseUrl(credentials, credentials == null ? null : credentials.get("_environment"));
    }

    // Temporary development bridge: if merchant credentials are missing, fall back to
    // application-level keys. Disable payment.providers.allow-global-credentials-fallback
    // outside local/dev and remove this fallback once merchant onboarding is complete.
    private String resolvePublicKey(ProviderCredentials credentials) {
        return credentials != null && credentials.publicKey() != null
                ? credentials.publicKey()
                : kkiapayProperties.getApi().getPublicKey();
    }

    private String resolveCallbackUrl(PayInProviderRequest request) {
        return request.getCallbackUrl() != null && !request.getCallbackUrl().isBlank()
                ? request.getCallbackUrl()
                : kkiapayProperties.getCallbackUrl();
    }

    private String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        if (key.contains("*")) return key;
        return key.substring(0, 4) + "...." + key.substring(key.length() - 4);
    }
}
