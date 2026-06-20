package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.KkiapayProperties;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KkiapayClientTest {

    private RestTemplate restTemplate;
    private KkiapayClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        KkiapayProperties properties = new KkiapayProperties();
        properties.getApi().setBaseUrl("https://api.kkiapay.me");
        properties.setCallbackUrl("https://fidelitypay.test/kkiapay/callback");
        client = new KkiapayClient(restTemplate, properties);
    }

    @Test
    void initiatePayIn_acceptsSandboxResponseWithInternalTransactionIdAndExtraFields() {
        String responseBody = """
                {
                  "internalTransactionId": "sandbox-transaction-1",
                  "status": "PENDING",
                  "providerCommonName": "MOOV",
                  "additionalProviderField": "ignored"
                }
                """;
        when(restTemplate.postForEntity(
                eq("https://api-sandbox.kkiapay.me/api/v1/payments/request"),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        PaymentResult result = client.initiatePayIn(PayInProviderRequest.builder()
                .credentials(new ProviderCredentials(Map.of("publicKey", "sandbox-public-key")))
                .environment("SANDBOX")
                .paymentId("fp_test")
                .amount(502)
                .country("CI")
                .operator("MOOV")
                .providerChannel("moov-ci")
                .flowType(PaymentFlowType.MOBILE_MONEY_REQUEST)
                .phone("2250776006060")
                .firstname("Test")
                .lastname("Client")
                .build());

        assertTrue(result.isSuccess());
        assertTrue(result.isProviderTransactionCreated());
        assertEquals("sandbox-transaction-1", result.getProviderId());
    }
}
