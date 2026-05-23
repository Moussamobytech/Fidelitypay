package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.PaydunyaProperties;
import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaActionsDTO;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaCustomerDTO;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaInvoiceDTO;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaRequestDTO;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaResponseDTO;
import com.Api.Fidelitypay.integration.paydunya.dto.PayDunyaStoreDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Component
@Slf4j
public class PayDunyaClient implements PayInProviderClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaydunyaProperties paydunyaProperties;

    public PayDunyaClient(RestTemplate restTemplate, PaydunyaProperties paydunyaProperties) {
        this.restTemplate = restTemplate;
        this.paydunyaProperties = paydunyaProperties;
    }

    @Override
    public String getProviderName() {
        return "PAYDUNYA";
    }

    public boolean isAvailable() {
        String baseUrl = paydunyaProperties.getApi().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return false;
        }
        try {
            restTemplate.getForEntity(baseUrl, String.class);
            return true;
        } catch (HttpStatusCodeException e) {
            return true;
        } catch (Exception e) {
            log.error("PayDunya availability check failed", e);
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
                .callbackUrl(paydunyaProperties.getCallbackUrl())
                .flowType(PaymentFlowType.HOSTED_CHECKOUT)
                .build());
    }

    @Override
    public PaymentResult initiatePayIn(PayInProviderRequest request) {
        long start = System.nanoTime();

        try {
            log.info("PayDunya Attempt | URL={} | MasterKey={} | PrivateKey={} | Token={}",
                    paydunyaProperties.getApi().getBaseUrl(),
                    mask(resolveMasterKey(request.getCredentials())),
                    mask(resolvePrivateKey(request.getCredentials())),
                    mask(resolveToken(request.getCredentials())));

            HttpHeaders headers = createHeaders(request.getCredentials());
            PayDunyaInvoiceDTO invoiceDTO = new PayDunyaInvoiceDTO(
                    request.getAmount(),
                    "FidelityPay payment " + safe(request.getPaymentId()),
                    java.util.List.of(resolveChannel(request)));
            invoiceDTO.setCustomer(new PayDunyaCustomerDTO(
                    (safe(request.getFirstname()) + " " + safe(request.getLastname())).trim(),
                    request.getEmail(),
                    request.getPhone()));

            PayDunyaRequestDTO payload = new PayDunyaRequestDTO(
                    invoiceDTO,
                    new PayDunyaStoreDTO(paydunyaProperties.getStore().getName()));
            payload.setActions(new PayDunyaActionsDTO(
                    resolveCallbackUrl(request),
                    request.getReturnUrl(),
                    request.getCancelUrl()));
            payload.setCustom_data(Map.of("paymentId", request.getPaymentId() != null ? request.getPaymentId() : ""));

            ResponseEntity<String> response = restTemplate.postForEntity(
                    paydunyaProperties.getApi().getBaseUrl() + "/checkout-invoice/create",
                    new HttpEntity<>(payload, headers),
                    String.class);

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            PaymentResult result = new PaymentResult();
            result.setRawResponse(response.getBody());
            result.setResponseTimeMs(elapsedMs);

            PayDunyaResponseDTO body = objectMapper.readValue(response.getBody(), PayDunyaResponseDTO.class);
            boolean success = "00".equals(body.getResponseCode());
            result.setSuccess(success);

            if (success) {
                result.setProviderId(body.getToken());
                result.setPaymentUrl(body.getResponseText());
                result.setProviderTransactionCreated(body.getToken() != null);
                log.info("PayDunya SUCCESS | token={} | timeMs={}", body.getToken(), elapsedMs);
            } else {
                log.warn("PayDunya FAILED | code={} | msg={}", body.getResponseCode(), body.getDescription());
                String code = body.getResponseCode();
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
            if (e instanceof org.springframework.web.client.ResourceAccessException
                    || e.getCause() instanceof SocketTimeoutException
                    || e.getCause() instanceof java.net.SocketException) {
                result.setProviderTransactionCreated(true);
            }
            if (e instanceof SocketTimeoutException || e.getCause() instanceof SocketTimeoutException) {
                result.setErrorType(ErrorType.TIMEOUT);
            } else if (e instanceof UnknownHostException || e.getCause() instanceof UnknownHostException) {
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

    @Override
    public PaymentStatus checkStatus(String providerPaymentId, ProviderCredentials credentials) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    paydunyaProperties.getApi().getBaseUrl() + "/checkout-invoice/confirm/" + providerPaymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders(credentials)),
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return PaymentStatus.PENDING_RECONCILIATION;
            }
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
            if (!"00".equals(root.path("response_code").asText())) {
                return PaymentStatus.PENDING_RECONCILIATION;
            }
            String status = root.path("status").asText("");
            return switch (status.toUpperCase()) {
                case "COMPLETED", "SUCCESS" -> PaymentStatus.SUCCESS;
                case "FAILED" -> PaymentStatus.FAILED;
                case "CANCELLED", "CANCELED" -> PaymentStatus.CANCELLED;
                default -> PaymentStatus.PENDING;
            };
        } catch (Exception e) {
            log.warn("PayDunya status check failed for token={}: {}", providerPaymentId, e.getMessage());
            return PaymentStatus.PENDING_RECONCILIATION;
        }
    }

    public boolean isValidCallbackHash(String hash) {
        return isValidCallbackHash(hash, null);
    }

    public boolean isValidCallbackHash(String hash, ProviderCredentials credentials) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] bytes = digest.digest(resolveMasterKey(credentials).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).equalsIgnoreCase(hash.trim());
        } catch (Exception e) {
            log.warn("Unable to validate PayDunya callback hash: {}", e.getMessage());
            return false;
        }
    }

    private java.util.List<String> getPayDunyaChannels(String operator, String country) {
        if (operator == null || country == null) return java.util.List.of("unknown");
        String op = operator.toUpperCase().trim();
        String c = country.toUpperCase().trim();
        if ("SN".equals(c) || "SENEGAL".equals(c)) {
            if ("WAVE".equals(op)) return java.util.List.of("wave-senegal");
            if ("OM".equals(op) || "ORANGE".equals(op)) return java.util.List.of("orange-money-senegal");
            if ("FREE".equals(op) || "MIXX".equals(op)) return java.util.List.of("free-money-senegal");
            if ("EXPRESSO".equals(op)) return java.util.List.of("expresso-sn");
        } else if ("CI".equals(c) || "COTE D'IVOIRE".equals(c) || "CIV".equals(c)) {
            if ("WAVE".equals(op)) return java.util.List.of("wave-ci");
            if ("OM".equals(op) || "ORANGE".equals(op)) return java.util.List.of("orange-money-ci");
            if ("MTN".equals(op)) return java.util.List.of("mtn-ci");
            if ("MOOV".equals(op)) return java.util.List.of("moov-ci");
            if ("TRESOR".equals(op) || "TRESORMONEY".equals(op)) return java.util.List.of("tresormoney-ci");
        } else if ("BJ".equals(c) || "BENIN".equals(c)) {
            if ("MTN".equals(op)) return java.util.List.of("mtn-benin");
            if ("MOOV".equals(op)) return java.util.List.of("moov-benin");
        } else if ("TG".equals(c) || "TOGO".equals(c)) {
            if ("TMONEY".equals(op) || "MIXX".equals(op) || "MIXXBYYAS".equals(op)) return java.util.List.of("t-money-togo");
            if ("MOOV".equals(op)) return java.util.List.of("moov-togo");
        } else if ("ML".equals(c) || "MALI".equals(c)) {
            if ("OM".equals(op) || "ORANGE".equals(op)) return java.util.List.of("orange-money-mali");
            if ("MOOV".equals(op)) return java.util.List.of("moov-ml");
            if ("SAMA".equals(op)) return java.util.List.of("sama-money");
        }
        return java.util.List.of(op.toLowerCase());
    }

    private String resolveChannel(PayInProviderRequest request) {
        if (request.getProviderChannel() != null && !request.getProviderChannel().isBlank()) {
            return request.getProviderChannel();
        }
        return getPayDunyaChannels(request.getOperator(), request.getCountry()).get(0);
    }

    private String resolveCallbackUrl(PayInProviderRequest request) {
        if (request.getCallbackUrl() != null && !request.getCallbackUrl().isBlank()) {
            return request.getCallbackUrl();
        }
        return paydunyaProperties.getCallbackUrl();
    }

    private HttpHeaders createHeaders(ProviderCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("PAYDUNYA-MASTER-KEY", resolveMasterKey(credentials));
        headers.set("PAYDUNYA-PRIVATE-KEY", resolvePrivateKey(credentials));
        headers.set("PAYDUNYA-TOKEN", resolveToken(credentials));
        return headers;
    }

    // Temporary development bridge: if merchant credentials are missing, fall back to
    // application-level keys. Disable payment.providers.allow-global-credentials-fallback
    // outside local/dev and remove this fallback once merchant onboarding is complete.
    private String resolveMasterKey(ProviderCredentials credentials) {
        return credentials != null && credentials.masterKey() != null
                ? credentials.masterKey()
                : paydunyaProperties.getApi().getMasterKey();
    }

    private String resolvePrivateKey(ProviderCredentials credentials) {
        return credentials != null && credentials.privateKey() != null
                ? credentials.privateKey()
                : paydunyaProperties.getApi().getPrivateKey();
    }

    private String resolveToken(ProviderCredentials credentials) {
        return credentials != null && credentials.token() != null
                ? credentials.token()
                : paydunyaProperties.getApi().getToken();
    }

    private String mask(String key) {
        if (key == null || key.length() < 8) return "****";
        if (key.contains("*")) return key;
        return key.substring(0, 4) + "...." + key.substring(key.length() - 4);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
