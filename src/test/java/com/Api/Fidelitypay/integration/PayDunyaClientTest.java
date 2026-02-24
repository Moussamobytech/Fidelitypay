package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.config.PaydunyaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PayDunyaClientTest {

        private RestTemplate restTemplate;
        private PayDunyaClient client;
        private PaydunyaProperties properties;

        @BeforeEach
        void setUp() throws Exception {
                restTemplate = mock(RestTemplate.class);
                properties = mock(PaydunyaProperties.class);
                PaydunyaProperties.Api api = mock(PaydunyaProperties.Api.class);
                PaydunyaProperties.Store store = mock(PaydunyaProperties.Store.class);

                when(properties.getApi()).thenReturn(api);
                when(properties.getStore()).thenReturn(store);
                when(api.getBaseUrl()).thenReturn("https://api.test.paydunya.local");
                when(api.getMasterKey()).thenReturn("pub");
                when(api.getPrivateKey()).thenReturn("priv");
                when(api.getToken()).thenReturn("master");
                when(store.getName()).thenReturn("TestStore");

                client = new PayDunyaClient(restTemplate, properties);
        }

        @Test
        void initiatePayment_successfulResponse_returnsTrue() {
                String jsonResponse = "{\"response_code\":\"00\", \"token\":\"test-token\", \"response_text\":\"http://payment.url\", \"description\":\"success\"}";
                ResponseEntity<String> response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

                when(restTemplate.postForEntity(eq("https://api.test.paydunya.local/checkout-invoice/create"),
                                any(HttpEntity.class), eq(String.class)))
                                .thenReturn(response);

                PaymentResult result = client.initiatePayment(100.0, "SN", "ORANGE_MONEY", "770000000", "John", "Doe",
                                "john.doe@example.com");
                assertNotNull(result);
                assertTrue(result.isSuccess());
                assertNotNull(result.getRawResponse());
        }

        @Test
        void initiatePayment_serverError_returnsFalse() {
                ResponseEntity<String> response = new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR);
                when(restTemplate.postForEntity(eq("https://api.test.paydunya.local/checkout-invoice/create"),
                                any(HttpEntity.class), eq(String.class)))
                                .thenReturn(response);

                PaymentResult result = client.initiatePayment(50.0, "SN", "WAVE", "770000000", "John", "Doe",
                                "john.doe@example.com");
                assertNotNull(result);
                assertFalse(result.isSuccess());
        }

        @Test
        void isAvailable_returnTrue_whenServerReachable() {
                when(restTemplate.getForEntity(any(String.class), eq(String.class)))
                                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
                assertTrue(client.isAvailable());
        }
}
