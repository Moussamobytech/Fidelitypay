package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.kkiapay.dto.KkiapayCallbackDTO;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderCallbackFinalizerTest {

    private PaymentRepository paymentRepository;
    private MerchantProviderAccountService providerAccountService;
    private KkiapayClient kkiapayClient;
    private PayDunyaClient payDunyaClient;
    private WebhookService webhookService;
    private LogEntryRepository logEntryRepository;
    private ProviderCallbackFinalizer finalizer;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        providerAccountService = mock(MerchantProviderAccountService.class);
        kkiapayClient = mock(KkiapayClient.class);
        payDunyaClient = mock(PayDunyaClient.class);
        webhookService = mock(WebhookService.class);
        logEntryRepository = mock(LogEntryRepository.class);
        finalizer = new ProviderCallbackFinalizer(paymentRepository, providerAccountService, kkiapayClient,
                payDunyaClient, webhookService, logEntryRepository, new PaymentStateTransitionService());
    }

    @Test
    void finalizeKkiapay_successTransitionsOnceAndSendsWebhook() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findByProviderPaymentIdForUpdate("kkiapay-1")).thenReturn(Optional.of(payment));
        when(kkiapayClient.checkStatus("kkiapay-1", null)).thenReturn(PaymentStatus.SUCCESS);

        finalizer.finalizeKkiapayCheckoutIpn(callback("kkiapay-1", true), "{\"transactionId\":\"kkiapay-1\"}");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(webhookService).sendWebhook(payment);
    }

    @Test
    void finalizeKkiapay_duplicateTerminalCallbackDoesNotSendWebhookAgain() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        when(paymentRepository.findByProviderPaymentIdForUpdate("kkiapay-1")).thenReturn(Optional.of(payment));

        finalizer.finalizeKkiapayCheckoutIpn(callback("kkiapay-1", true), "{\"transactionId\":\"kkiapay-1\"}");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository, never()).save(payment);
        verify(webhookService, never()).sendWebhook(any(Payment.class));
    }

    @Test
    void finalizeKkiapay_conflictingTerminalCallbackDoesNotOverwriteSuccess() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        when(paymentRepository.findByProviderPaymentIdForUpdate("kkiapay-1")).thenReturn(Optional.of(payment));

        finalizer.finalizeKkiapayCheckoutIpn(callback("kkiapay-1", false), "{\"transactionId\":\"kkiapay-1\"}");

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(paymentRepository, never()).save(payment);
        verify(webhookService, never()).sendWebhook(any(Payment.class));
    }

    @Test
    void finalizePayDunya_pendingReconciliationCanBecomeFailed() {
        Payment payment = payment(PaymentStatus.PENDING_RECONCILIATION);
        when(paymentRepository.findByProviderPaymentIdForUpdate("paydunya-token")).thenReturn(Optional.of(payment));
        when(payDunyaClient.checkStatus("paydunya-token", null)).thenReturn(PaymentStatus.PENDING_RECONCILIATION);

        finalizer.finalizePayDunyaCheckoutIpn("paydunya-token", "FAILED", "status=FAILED");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(webhookService).sendWebhook(payment);
    }

    private KkiapayCallbackDTO callback(String transactionId, boolean success) {
        KkiapayCallbackDTO callback = new KkiapayCallbackDTO();
        callback.setTransactionId(transactionId);
        callback.setPaymentSucces(success);
        return callback;
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setPaymentId("fp_test");
        payment.setProviderPaymentId("provider-1");
        payment.setProvider("KKIAPAY");
        payment.setRouteName("KKIAPAY_WAVE_SN");
        payment.setStatus(status);
        payment.setAmount(BigDecimal.valueOf(1000));
        payment.setCost(BigDecimal.ZERO);
        payment.setCurrency("XOF");
        payment.setCountry("SN");
        payment.setOperator("WAVE");
        return payment;
    }
}
