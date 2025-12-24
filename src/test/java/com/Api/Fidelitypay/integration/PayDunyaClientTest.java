package com.Api.Fidelitypay.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class PayDunyaClientTest {

    private RestTemplate restTemplate;
    private PayDunyaClient client;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = mock(RestTemplate.class);
        client = new PayDunyaClient(restTemplate);

        // Set private fields via reflection
        setField("baseUrl", "https://api.test.paydunya.local");
        setField("publicKey", "pub");
        setField("privateKey", "priv");
        setField("masterToken", "master");
        setField("storeName", "TestStore");
    }

    private void setField(String name, String value) throws Exception {
        Field f = PayDunyaClient.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(client, value);
    }

    @Test
    void initiatePayment_successfulResponse_returnsTrue() {
        ResponseEntity<String> response = new ResponseEntity<>("{\"status\":\"ok\"}", HttpStatus.OK);
        when(restTemplate.exchange(eq("https://api.test.paydunya.local/checkout-invoice"), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        PaymentResult result = client.initiatePayment(100.0, "SN", "ORANGE_MONEY");
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("{\"status\":\"ok\"}", result.getRawResponse());
    }

    @Test
    void initiatePayment_serverError_returnsFalse() {
        ResponseEntity<String> response = new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(eq("https://api.test.paydunya.local/checkout-invoice"), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        PaymentResult result = client.initiatePayment(50.0, "SN", "WAVE");
        assertNotNull(result);
        assertFalse(result.isSuccess());
    }

    @Test
    void isAvailable_when200_returnsTrue() {
        when(restTemplate.getForEntity("https://api.test.paydunya.local/health", String.class))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));
        assertTrue(client.isAvailable());
    }

    @Test
    void isAvailable_onException_returnsFalse() {
        when(restTemplate.getForEntity("https://api.test.paydunya.local/health", String.class))
                .thenThrow(new RuntimeException("down"));
        assertFalse(client.isAvailable());
    }
}
