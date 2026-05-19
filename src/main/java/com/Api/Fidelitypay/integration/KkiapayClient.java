package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.KkiapayProperties;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayRequestDTO;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayResponseDTO;
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
        String baseUrl = kkiapayProperties.getApi().getBaseUrl();
        if (baseUrl == null) {
            return false;
        }
        try {
            restTemplate.getForEntity(baseUrl, String.class);
            return true;
        } catch (HttpStatusCodeException e) {
            // 4xx or 5xx means server responded
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
            // 🔐 Headers
	HttpHeaders headers = new HttpHeaders();
	headers.setContentType(MediaType.APPLICATION_JSON);

	headers.set("x-api-key", kkiapayProperties.getApi().getPublicKey());
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

            if (isWave) {
                payloadBuilder.email(request.getEmail())
                        .name((safe(request.getFirstname()) + " " + safe(request.getLastname())).trim())
                        .success_url(request.getReturnUrl() != null ? request.getReturnUrl() : resolveCallbackUrl(request))
                        .error_url(request.getCancelUrl() != null ? request.getCancelUrl() : resolveCallbackUrl(request));
            } else {
                payloadBuilder.phoneNumber(request.getPhone())
                        .firstname(request.getFirstname())
                        .lastname(request.getLastname());
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

            return result;

        } catch (Exception e) {
            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            log.error("Kkiapay ERROR | timeMs={}", elapsedMs, e);

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
    public PaymentResult validateAction(Payment payment, String actionType, String value) {
        if (!"SUBMIT_OTP".equalsIgnoreCase(actionType)) {
            return PayInProviderClient.super.validateAction(payment, actionType, value);
        }
        long start = System.nanoTime();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", kkiapayProperties.getApi().getPublicKey());
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
    public PaymentStatus checkStatus(String providerPaymentId) {
        KkiapayResponseDTO status = checkTransactionStatus(providerPaymentId);
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

    private String resolveCallbackUrl(PayInProviderRequest request) {
        return request.getCallbackUrl() != null ? request.getCallbackUrl() : kkiapayProperties.getCallbackUrl();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
