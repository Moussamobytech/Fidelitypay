package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.enums.PaymentFlowType;
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

import java.util.List;

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
                                new PaymentStateTransitionService());
        }

        private com.Api.Fidelitypay.model.PaymentProviderRoute route(String code) {
                com.Api.Fidelitypay.model.PaymentProvider provider = new com.Api.Fidelitypay.model.PaymentProvider();
                provider.setCode(code);
                com.Api.Fidelitypay.model.PaymentProviderRoute route = new com.Api.Fidelitypay.model.PaymentProviderRoute();
                route.setProvider(provider);
                route.setCountry("BJ");
                route.setOperator("MTN");
                route.setFlowType(PaymentFlowType.MOBILE_MONEY_REQUEST);
                route.setProviderChannel(code.toLowerCase() + "-channel");
                route.setCost(0.0);
                return route;
        }

        @Test
        void initiatePayment_kkiapayPrimarySuccess() {
                // KKIAPAY est le 1er provider essayé
                PaymentResult pr = new PaymentResult(true);
                pr.setProviderId("kkiapay-123");
                pr.setRawResponse("{\"status\":\"ok\"}");
                pr.setResponseTimeMs(150.0);

                when(kkiapayClient.initiatePayIn(any())).thenReturn(pr);

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
        }

        @Test
        void initiatePayment_kkiapayFailsThenPaydunyaSuccess_keepsPaymentPendingUntilCallback() {
                // KKIAPAY échoue (erreur technique)
                PaymentResult kkiapayResult = new PaymentResult(false);
                kkiapayResult.setRawResponse("TIMEOUT: Connection failed");
                kkiapayResult.setResponseTimeMs(3000.0);
                kkiapayResult.setErrorType(ErrorType.TIMEOUT);
                when(kkiapayClient.initiatePayIn(any())).thenReturn(kkiapayResult);

                // PAYDUNYA réussit (fallback)
                PaymentResult paydunyaResult = new PaymentResult(true);
                paydunyaResult.setProviderId("paydunya-success-456");
                paydunyaResult.setRawResponse("{\"status\":\"success\"}");
                paydunyaResult.setResponseTimeMs(200.0);
                when(payDunyaClient.initiatePayIn(any())).thenReturn(paydunyaResult);

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

                verify(kkiapayClient).initiatePayIn(any());
                verify(payDunyaClient).initiatePayIn(any());
        }

        @Test
        void initiatePayment_allProvidersFail_setsFailedStatus() {
                PaymentResult failResult = new PaymentResult(false);
                failResult.setRawResponse("Erreur: Solde insuffisant pour la transaction");
                failResult.setResponseTimeMs(100.0);

                when(kkiapayClient.initiatePayIn(any())).thenReturn(failResult);
                when(payDunyaClient.initiatePayIn(any())).thenReturn(failResult);

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
        void initiatePayment_usesSelectedRouteChannelAndFlowType() {
                com.Api.Fidelitypay.model.PaymentProviderRoute route = route("PAYDUNYA");
                route.setCountry("SN");
                route.setOperator("YAS");
                route.setFlowType(PaymentFlowType.HOSTED_CHECKOUT);
                route.setProviderChannel("free-money-senegal");

                when(routeService.findAvailablePayIn(eq("SN"), eq("YAS"), eq("LIVE"), any())).thenReturn(List.of(route));

                PaymentResult pr = new PaymentResult(true);
                pr.setProviderId("paydunya-token");
                pr.setRawResponse("{\"response_code\":\"00\"}");
                when(payDunyaClient.initiatePayIn(any())).thenReturn(pr);
                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                Payment res = paymentService.initiatePayment(new User(), 100.0, "SN", "YAS", "770000000",
                                "John", "Doe", "john@example.com");

                ArgumentCaptor<PayInProviderRequest> captor = ArgumentCaptor.forClass(PayInProviderRequest.class);
                verify(payDunyaClient).initiatePayIn(captor.capture());

                PayInProviderRequest request = captor.getValue();
                assertEquals("free-money-senegal", request.getProviderChannel());
                assertEquals(PaymentFlowType.HOSTED_CHECKOUT, request.getFlowType());
                assertEquals("YAS", request.getOperator());
                assertEquals("SN", request.getCountry());
                assertEquals("PAYDUNYA_YAS_SN", res.getRouteName());
                assertEquals("free-money-senegal", res.getProviderChannel());
                assertEquals(PaymentFlowType.HOSTED_CHECKOUT.name(), res.getFlowType());
        }

        @Test
        void initiatePayment_acceptsSenegalPhoneWithCountryPrefix() {
                com.Api.Fidelitypay.model.PaymentProviderRoute route = route("PAYDUNYA");
                route.setCountry("SN");
                route.setOperator("WAVE");
                route.setProviderChannel("wave-senegal");

                when(routeService.findAvailablePayIn(eq("SN"), eq("WAVE"), eq("LIVE"), any())).thenReturn(List.of(route));

                PaymentResult pr = new PaymentResult(true);
                pr.setProviderId("paydunya-token");
                pr.setRawResponse("{\"response_code\":\"00\"}");
                when(payDunyaClient.initiatePayIn(any())).thenReturn(pr);
                when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

                Payment res = paymentService.initiatePayment(new User(), 100.0, "SN", "WAVE", "221776006060",
                                "John", "Doe", "john@example.com");

                assertNotNull(res);
                assertNotEquals("INVALID_PHONE_NUMBER", res.getFailureReason());
                verify(payDunyaClient).initiatePayIn(any());
        }

}
