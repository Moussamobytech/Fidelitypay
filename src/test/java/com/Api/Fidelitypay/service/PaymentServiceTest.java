package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
class PaymentServiceTest {

        private PaymentRepository paymentRepository;
        private LogEntryRepository logEntryRepository;
        private WebhookService webhookService;
        private KkiapayClient kkiapayClient;
        private PayDunyaClient payDunyaClient;
        private RouteSelectionService routeSelectionService;
        private PaymentService paymentService;

        @BeforeEach
        void setUp() {
                paymentRepository = mock(PaymentRepository.class);
                logEntryRepository = mock(LogEntryRepository.class);
                webhookService = mock(WebhookService.class);
                kkiapayClient = mock(KkiapayClient.class);
                payDunyaClient = mock(PayDunyaClient.class);
                routeSelectionService = mock(RouteSelectionService.class);

                com.Api.Fidelitypay.model.Route route1 = new com.Api.Fidelitypay.model.Route();
                route1.setProvider("KKIAPAY");
                com.Api.Fidelitypay.model.Route route2 = new com.Api.Fidelitypay.model.Route();
                route2.setProvider("PAYDUNYA");
                when(routeSelectionService.getSortedRoutes(anyString(), anyString())).thenReturn(java.util.List.of(route1, route2));

                paymentService = new PaymentService(paymentRepository, logEntryRepository,
                                webhookService, kkiapayClient, payDunyaClient, routeSelectionService);
        }

        @Test
        void initiatePayment_kkiapayPrimarySuccess() {
                // KKIAPAY est le 1er provider essayé
                PaymentResult pr = new PaymentResult(true);
                pr.setProviderId("kkiapay-123");
                pr.setRawResponse("{\"status\":\"ok\"}");
                pr.setResponseTimeMs(150.0);

                when(kkiapayClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(pr);

                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                User user = new User();
                Payment res = paymentService.initiatePayment(user, 100.0, "BJ", "MTN", "22961000000", "John", "Doe",
                                "john@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.SUCCESS, res.getStatus());
                assertEquals("KKIAPAY", res.getProvider());
                assertFalse(res.isUsedFallback());
                verify(paymentRepository, atLeastOnce()).save(any());
        }

        @Test
        void initiatePayment_kkiapayFailsThenPaydunjaSuccess() {
                // KKIAPAY échoue (erreur technique)
                PaymentResult kkiapayResult = new PaymentResult(false);
                kkiapayResult.setRawResponse("TIMEOUT: Connection failed");
                kkiapayResult.setResponseTimeMs(3000.0);
                kkiapayResult.setErrorType(ErrorType.TIMEOUT);
                when(kkiapayClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(kkiapayResult);

                // PAYDUNYA réussit (fallback)
                PaymentResult paydunyaResult = new PaymentResult(true);
                paydunyaResult.setProviderId("paydunya-success-456");
                paydunyaResult.setRawResponse("{\"status\":\"success\"}");
                paydunyaResult.setResponseTimeMs(200.0);
                when(payDunyaClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(paydunyaResult);

                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                User user = new User();
                Payment res = paymentService.initiatePayment(user, 100.0, "BJ", "MTN", "22961000000", "John", "Doe",
                                "john@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.SUCCESS, res.getStatus());
                assertEquals("PAYDUNYA", res.getProvider());
                assertTrue(res.isUsedFallback());

                verify(kkiapayClient).initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString());
                verify(payDunyaClient).initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString());
        }

        @Test
        void initiatePayment_allProvidersFail_setsFailedStatus() {
                PaymentResult failResult = new PaymentResult(false);
                failResult.setRawResponse("Erreur: Solde insuffisant pour la transaction");
                failResult.setResponseTimeMs(100.0);

                when(kkiapayClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(failResult);
                when(payDunyaClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(failResult);

                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                User user = new User();
                Payment res = paymentService.initiatePayment(user, 500.0, "BJ", "MTN", "22961000000", "Jane", "Smith",
                                "jane@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.FAILED, res.getStatus());
                assertNotNull(res.getFailureReason());
                verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        }
}
