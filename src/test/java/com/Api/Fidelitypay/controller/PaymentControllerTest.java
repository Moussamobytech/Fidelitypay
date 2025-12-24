package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.PaymentInitiateRequest;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@SuppressWarnings("null")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private PaymentService paymentService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public PaymentService paymentService() {
            return Mockito.mock(PaymentService.class);
        }
    }

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    void initiatePayment_invalidRequest_returns400() throws Exception {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setAmount(0.0);
        req.setCountry("");
        req.setOperator("");

        mockMvc.perform(post("/api/payments/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void initiatePayment_valid_returns200() throws Exception {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setAmount(100.0);
        req.setCountry("SN");
        req.setOperator("WAVE");

        when(paymentService.initiatePayment(anyDouble(), anyString(), anyString())).thenReturn(new Payment());

        mockMvc.perform(post("/api/payments/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
