package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.MerchantPaymentResponse;
import com.Api.Fidelitypay.controller.dto.MerchantApiPrincipal;
import com.Api.Fidelitypay.controller.dto.PaymentInitiateRequest;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.service.MerchantPayInService;
import com.Api.Fidelitypay.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import com.Api.Fidelitypay.model.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardPaymentController.class)
@SuppressWarnings("null")
class DashboardPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private MerchantPayInService merchantPayInService;

    @MockBean
    private com.Api.Fidelitypay.repository.ApiKeyRepository apiKeyRepository;

    @MockBean
    private com.Api.Fidelitypay.service.DeveloperMetricsService developerMetricsService;

    @MockBean
    private com.Api.Fidelitypay.service.ApiKeyService apiKeyService;

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
        authenticateDeveloper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void initiatePayment_invalidRequest_returns400() throws Exception {
        PaymentInitiateRequest req = new PaymentInitiateRequest();
        req.setAmount(0.0);
        req.setCountry("");
        req.setOperator("");

        mockMvc.perform(post("/api/payments/initiate")
                .with(csrf())
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
        req.setPhone("771111111");
        req.setEnvironment("sandbox");

        ApiKey apiKey = ApiKey.builder()
                .id("key-1")
                .userId("user-1")
                .name("Sandbox key")
                .publicKey("pk_test")
                .secretKeyHash("hash")
                .isActive(true)
                .build();
        when(apiKeyRepository.findByUserIdAndIsActive("user-1", true))
                .thenReturn(java.util.List.of(apiKey));
        when(merchantPayInService.initiate(any(), any(), anyString()))
                .thenReturn(MerchantPaymentResponse.builder()
                        .paymentId("fp_test")
                        .status(PaymentStatus.PENDING)
                        .country("SN")
                        .operator("WAVE")
                        .build());

        mockMvc.perform(post("/api/payments/initiate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        ArgumentCaptor<MerchantApiPrincipal> principalCaptor = ArgumentCaptor.forClass(MerchantApiPrincipal.class);
        verify(merchantPayInService).initiate(principalCaptor.capture(), any(), anyString());
        assertEquals("DASHBOARD", principalCaptor.getValue().getInitiationSource());
        assertEquals("SANDBOX", principalCaptor.getValue().getEnvironment());
    }

    private void authenticateDeveloper() {
        User user = User.builder()
                .id("user-1")
                .email("dev@example.com")
                .password("x")
                .fullName("Dev")
                .role(User.Role.DEVELOPER)
                .isActive(true)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
