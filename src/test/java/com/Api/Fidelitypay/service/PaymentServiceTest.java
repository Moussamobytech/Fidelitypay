package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.Route;
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
        private RouteSelectionService routeSelectionService;
        private WebhookService webhookService;
        private KkiapayClient kkiapayClient;
        private PayDunyaClient payDunyaClient;
        private PaymentService paymentService;

        @BeforeEach
        void setUp() {
                paymentRepository = mock(PaymentRepository.class);
                logEntryRepository = mock(LogEntryRepository.class);
                routeSelectionService = mock(RouteSelectionService.class);
                webhookService = mock(WebhookService.class);
                kkiapayClient = mock(KkiapayClient.class);
                payDunyaClient = mock(PayDunyaClient.class);

                paymentService = new PaymentService(paymentRepository, logEntryRepository, routeSelectionService,
                                webhookService, kkiapayClient, payDunyaClient);
        }

        @Test
        void initiatePayment_primarySuccess_persistsPayment() {
                Route route = new Route();
                route.setName("PAYDUNYA_WAVE");
                route.setProvider("PAYDUNYA");
                route.setCost(0.1);

                when(routeSelectionService.selectBestRoute(anyString())).thenReturn(route);

                PaymentResult pr = new PaymentResult(true);
                pr.setProviderId("paydunya-123");
                pr.setRawResponse("{\"status\":\"ok\"}");
                pr.setResponseTimeMs(150.0);

                when(payDunyaClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(pr);

                Payment saved = new Payment();
                when(paymentRepository.save(any())).thenReturn(saved);

                Payment res = paymentService.initiatePayment(100.0, "SN", "WAVE", "221770000000", "John", "Doe",
                                "john@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.SUCCESS, res.getStatus());
                verify(paymentRepository, atLeastOnce()).save(any());
        }

        @Test
        void initiatePayment_failure_setsFailureReason() {
                Route route = new Route();
                route.setName("PAYDUNYA_WAVE");
                route.setProvider("PAYDUNYA");
                route.setCost(0.1);

                when(routeSelectionService.selectBestRoute(anyString())).thenReturn(route);

                PaymentResult pr = new PaymentResult(false);
                pr.setProviderId("paydunya-error");
                pr.setRawResponse("Erreur: Solde insuffisant pour la transaction");
                pr.setResponseTimeMs(100.0);

                when(payDunyaClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(pr);

                Payment savedPayment = new Payment();
                when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                        Payment p = invocation.getArgument(0);
                        return p;
                });

                Payment res = paymentService.initiatePayment(500.0, "SN", "WAVE", "221770000000", "Jane", "Smith",
                                "jane@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.FAILED, res.getStatus());
                assertEquals("INSUFFICIENT_FUNDS", res.getFailureReason());

                verify(paymentRepository, atLeastOnce()).save(any(Payment.class));

                // Verify LogEntry
                org.mockito.ArgumentCaptor<com.Api.Fidelitypay.model.LogEntry> logCaptor = org.mockito.ArgumentCaptor
                                .forClass(com.Api.Fidelitypay.model.LogEntry.class);
                verify(logEntryRepository).save(logCaptor.capture());
                assertEquals("INSUFFICIENT_FUNDS", logCaptor.getValue().getFailureReason());
        }

        @Test
        void initiatePayment_fallbackSuccess_usesSecondaryRoute() {
                // 1. Setup routes
                Route primary = new Route();
                primary.setName("PAYDUNYA_WAVE");
                primary.setProvider("PAYDUNYA");
                primary.setCost(0.1);

                Route fallback = new Route();
                fallback.setName("KKIAPAY_WAVE");
                fallback.setProvider("KKIAPAY");
                fallback.setCost(0.2);

                when(routeSelectionService.getSortedRoutes(anyString()))
                                .thenReturn(java.util.List.of(primary, fallback));

                // 2. Mock primary failure (Technical error)
                PaymentResult primaryResult = new PaymentResult(false);
                primaryResult.setRawResponse("TIMEOUT: Connection failed");
                primaryResult.setResponseTimeMs(3000.0);
                primaryResult.setErrorType(com.Api.Fidelitypay.enums.ErrorType.TIMEOUT);
                when(payDunyaClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(primaryResult);

                // 3. Mock fallback success
                PaymentResult fallbackResult = new PaymentResult(true);
                fallbackResult.setProviderId("kkiapay-success-123");
                fallbackResult.setRawResponse("{\"status\":\"success\"}");
                fallbackResult.setResponseTimeMs(200.0);
                when(kkiapayClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString())).thenReturn(fallbackResult);

                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                // Execute
                Payment res = paymentService.initiatePayment(100.0, "SN", "WAVE", "221770000000", "John", "Doe",
                                "john@example.com");

                // Verify
                assertNotNull(res);
                assertEquals(PaymentStatus.SUCCESS, res.getStatus());
                assertEquals("KKIAPAY_WAVE", res.getRouteName());
                assertEquals("KKIAPAY", res.getProvider());
                assertTrue(res.isUsedFallback());

                verify(payDunyaClient).initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString());
                verify(kkiapayClient).initiatePayment(anyDouble(), anyString(), anyString(), anyString(), anyString(),
                                anyString(), anyString());
        }
}
