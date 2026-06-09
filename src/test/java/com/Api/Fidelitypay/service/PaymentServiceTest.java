package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.PayInProviderRequest;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@SuppressWarnings("null")
class PaymentServiceTest {

        private PaymentRepository paymentRepository;
        private LogEntryRepository logEntryRepository;
        private WebhookService webhookService;
        private KkiapayClient kkiapayClient;
        private PayDunyaClient payDunyaClient;
        private PaymentRouteService routeService;
        private PaymentService paymentService;

        @BeforeEach
        void setUp() {
                paymentRepository = mock(PaymentRepository.class);
                logEntryRepository = mock(LogEntryRepository.class);
                webhookService = mock(WebhookService.class);
                kkiapayClient = mock(KkiapayClient.class);
                payDunyaClient = mock(PayDunyaClient.class);
                routeService = mock(PaymentRouteService.class);

                com.Api.Fidelitypay.model.PaymentProviderRoute route1 = route("KKIAPAY");
                com.Api.Fidelitypay.model.PaymentProviderRoute route2 = route("PAYDUNYA");
                when(routeService.findAvailablePayIn(anyString(), anyString(), eq("LIVE"), any())).thenReturn(java.util.List.of(route1, route2));

                paymentService = new PaymentService(paymentRepository, logEntryRepository,
                                webhookService, kkiapayClient, payDunyaClient, routeService,
                                new com.Api.Fidelitypay.service.failure.PaymentFailureClassifier());
        }

        private com.Api.Fidelitypay.model.PaymentProviderRoute route(String code) {
                com.Api.Fidelitypay.model.PaymentProvider provider = new com.Api.Fidelitypay.model.PaymentProvider();
                provider.setCode(code);
                com.Api.Fidelitypay.model.PaymentProviderRoute route = new com.Api.Fidelitypay.model.PaymentProviderRoute();
                route.setProvider(provider);
                route.setCountry("BJ");
                route.setOperator("MTN");
                route.setFlowType("PAYDUNYA".equals(code) ? PaymentFlowType.HOSTED_CHECKOUT : PaymentFlowType.MOBILE_MONEY_REQUEST);
                route.setProviderChannel("PAYDUNYA".equals(code) ? "mtn-benin" : "momo");
                return route;
        }

        @Test
        void initiatePayment_kkiapayPrimarySuccess() {
                // KKIAPAY est le 1er provider essayé
                PaymentResult pr = new PaymentResult(true);
                pr.setProviderId("kkiapay-123");
                pr.setRawResponse("{\"status\":\"ok\"}");
                pr.setResponseTimeMs(150.0);

                when(kkiapayClient.initiatePayIn(any(PayInProviderRequest.class))).thenReturn(pr);

                when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                User user = new User();
                Payment res = paymentService.initiatePayment(user, 100.0, "BJ", "MTN", "61000000", "John", "Doe",
                                "john@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.PENDING, res.getStatus());
                assertEquals("KKIAPAY", res.getProvider());
                assertEquals("kkiapay-123", res.getProviderPaymentId());
                assertFalse(res.isUsedFallback());
                verify(paymentRepository, atLeastOnce()).save(any());
                verify(webhookService, never()).sendWebhook(any(Payment.class));

                ArgumentCaptor<PayInProviderRequest> requestCaptor = ArgumentCaptor.forClass(PayInProviderRequest.class);
                verify(kkiapayClient).initiatePayIn(requestCaptor.capture());
                assertEquals(res.getPaymentId(), requestCaptor.getValue().getPaymentId());
                assertEquals(PaymentFlowType.MOBILE_MONEY_REQUEST, requestCaptor.getValue().getFlowType());
        }

        @Test
        void initiatePayment_kkiapayFailsThenPaydunyaSuccess_keepsPaymentPendingUntilCallback() {
                // KKIAPAY échoue (erreur technique)
                PaymentResult kkiapayResult = new PaymentResult(false);
                kkiapayResult.setRawResponse("TIMEOUT: Connection failed");
                kkiapayResult.setResponseTimeMs(3000.0);
                kkiapayResult.setErrorType(ErrorType.TIMEOUT);
                when(kkiapayClient.initiatePayIn(any(PayInProviderRequest.class))).thenReturn(kkiapayResult);

                // PAYDUNYA réussit (fallback)
                PaymentResult paydunyaResult = new PaymentResult(true);
                paydunyaResult.setProviderId("paydunya-success-456");
                paydunyaResult.setRawResponse("{\"status\":\"success\"}");
                paydunyaResult.setResponseTimeMs(200.0);
                when(payDunyaClient.initiatePayIn(any(PayInProviderRequest.class))).thenReturn(paydunyaResult);

                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                User user = new User();
                Payment res = paymentService.initiatePayment(user, 100.0, "BJ", "MTN", "61000000", "John", "Doe",
                                "john@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.PENDING, res.getStatus());
                assertEquals("PAYDUNYA", res.getProvider());
                assertEquals("paydunya-success-456", res.getProviderPaymentId());
                assertTrue(res.isUsedFallback());
                verify(webhookService, never()).sendWebhook(any(Payment.class));

                verify(kkiapayClient).initiatePayIn(any(PayInProviderRequest.class));
                verify(payDunyaClient).initiatePayIn(any(PayInProviderRequest.class));
        }

        @Test
        void initiatePayment_allProvidersFail_setsFailedStatus() {
                PaymentResult failResult = new PaymentResult(false);
                failResult.setRawResponse("Erreur: Solde insuffisant pour la transaction");
                failResult.setResponseTimeMs(100.0);

                when(kkiapayClient.initiatePayIn(any(PayInProviderRequest.class))).thenReturn(failResult);
                when(payDunyaClient.initiatePayIn(any(PayInProviderRequest.class))).thenReturn(failResult);

                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                User user = new User();
                Payment res = paymentService.initiatePayment(user, 500.0, "BJ", "MTN", "61000000", "Jane", "Smith",
                                "jane@example.com");

                assertNotNull(res);
                assertEquals(PaymentStatus.FAILED, res.getStatus());
                assertNotNull(res.getFailureReason());
                verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
                verify(webhookService).sendWebhook(any(Payment.class));
        }

        @Test
        void processKkiapayCallback_pendingPayment_marksSuccessAndSendsWebhook() {
                Payment payment = new Payment();
                payment.setStatus(PaymentStatus.PENDING);
                payment.setProviderPaymentId("kkiapay-123");

                when(paymentRepository.findByProviderPaymentId("kkiapay-123")).thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO callback =
                                new com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO();
                callback.setTransactionId("kkiapay-123");
                callback.setPaymentSucces(true);

                paymentService.processKkiapayCallback(callback);

                assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
                assertNull(payment.getFailureReason());
                verify(paymentRepository).save(payment);
                verify(webhookService).sendWebhook(payment);
        }

        @Test
        void processPayDunyaCallback_pendingPayment_marksFailedAndSendsWebhook() {
                Payment payment = new Payment();
                payment.setStatus(PaymentStatus.PENDING);
                payment.setProviderPaymentId("paydunya-token");

                when(paymentRepository.findByProviderPaymentId("paydunya-token")).thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                paymentService.processPayDunyaCallback("paydunya-token", false);

                assertEquals(PaymentStatus.FAILED, payment.getStatus());
                assertEquals("PROVIDER_REPORTED_FAILURE", payment.getFailureReason());
                verify(paymentRepository).save(payment);
                verify(webhookService).sendWebhook(payment);
        }

        @Test
        void processPayDunyaCallback_terminalPayment_doesNotOverwriteFinalStatus() {
                Payment payment = new Payment();
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setProviderPaymentId("paydunya-token");

                when(paymentRepository.findByProviderPaymentId("paydunya-token")).thenReturn(Optional.of(payment));

                paymentService.processPayDunyaCallback("paydunya-token", false);

                assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
                verify(paymentRepository, never()).save(any(Payment.class));
                verify(webhookService, never()).sendWebhook(any(Payment.class));
        }
}
