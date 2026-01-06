package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.Enum.PaymentStatus;
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
    private KkiapayClient samirPayClient;
    private PayDunyaClient payDunyaClient;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        logEntryRepository = mock(LogEntryRepository.class);
        routeSelectionService = mock(RouteSelectionService.class);
        webhookService = mock(WebhookService.class);
        samirPayClient = mock(KkiapayClient.class);
        payDunyaClient = mock(PayDunyaClient.class);

        paymentService = new PaymentService(paymentRepository, logEntryRepository, routeSelectionService,
                webhookService, samirPayClient, payDunyaClient);
    }

    @Test
    void initiatePayment_primarySuccess_persistsPayment() {
        Route route = new Route();
        route.setName("PAYDUNYA_WAVE");
        route.setCost(0.1);

        when(routeSelectionService.selectBestRoute(anyString())).thenReturn(route);

        PaymentResult pr = new PaymentResult(true);
        pr.setProviderId("paydunya-123");
        pr.setRawResponse("{\"status\":\"ok\"}");
        pr.setResponseTimeMs(150.0);

        when(payDunyaClient.initiatePayment(anyDouble(), anyString(), anyString(), anyString())).thenReturn(pr);

        Payment saved = new Payment();
        when(paymentRepository.save(any())).thenReturn(saved);

        Payment res = paymentService.initiatePayment(100.0, "SN", "WAVE", "221770000000");

        assertNotNull(res);
        assertEquals(PaymentStatus.SUCCESS, res.getStatus());
        verify(paymentRepository, atLeastOnce()).save(any());
    }

}
