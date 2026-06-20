package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderCallbackServiceTest {

    private PaymentRepository paymentRepository;
    private MerchantProviderAccountService providerAccountService;
    private PayDunyaClient payDunyaClient;
    private ProviderCallbackFinalizer finalizer;
    private ProviderCallbackService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        providerAccountService = mock(MerchantProviderAccountService.class);
        payDunyaClient = mock(PayDunyaClient.class);
        finalizer = mock(ProviderCallbackFinalizer.class);
        service = new ProviderCallbackService(paymentRepository, providerAccountService, payDunyaClient, finalizer);
    }

    @Test
    void acceptPayDunyaCheckoutIpn_invalidHashDoesNotFinalize() {
        Payment payment = new Payment();
        payment.setPaymentId("fp_test");
        payment.setProviderPaymentId("paydunya-token");
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByProviderPaymentId("paydunya-token")).thenReturn(Optional.of(payment));
        when(payDunyaClient.isValidCallbackHash("bad-hash", null)).thenReturn(false);

        String result = service.acceptPayDunyaCheckoutIpn("paydunya-token", "COMPLETED", "bad-hash", "raw");

        assertEquals("ignored", result);
        verify(finalizer, never()).finalizePayDunyaCheckoutIpn(any(), any(), any());
    }
}
