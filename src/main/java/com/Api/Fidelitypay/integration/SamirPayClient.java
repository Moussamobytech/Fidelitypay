package com.Api.Fidelitypay.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.util.Objects;
import com.Api.Fidelitypay.integration.samirpay.dto.SamirPayRequest;
import java.util.HashMap;
import java.util.Map;

@Component
public class SamirPayClient {

    private final RestTemplate restTemplate;

    @Value("${samirpay.api.base-url}")
    private String baseUrl;

    @Value("${samirpay.api.public-key}")
    private String publicKey;

    @Value("${samirpay.api.private-key}")
    private String privateKey;

    @Value("${samirpay.api.token}")
    private String token;

    public SamirPayClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isAvailable() {
        try {
            // Simulation: always true for now, or check a real health endpoint if available
            // ResponseEntity<String> response = restTemplate.getForEntity(baseUrl +
            // "/health", String.class);
            // return response.getStatusCode().is2xxSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean initiatePayment(double amount, String country, String operator) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", token);
            // Add other headers as required by actual SamirPay API, e.g.:
            // headers.set("public-key", publicKey);

            SamirPayRequest request = new SamirPayRequest();
            request.setAmount(amount);
            request.setCurrency("XOF");
            request.setOperator(operator);
            request.setDescription("Payment for " + operator);

            HttpEntity<SamirPayRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/payment/initiate", // URL to be verified
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("SamirPay Payment Success: " + response.getBody());
                return true;
            } else {
                System.out.println("SamirPay Payment Failed: " + response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            System.out.println("SamirPay Payment Error: " + e.getMessage());
            return false;
        }
    }
}