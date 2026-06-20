package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.model.Payment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStateTransitionServiceTest {

    private final PaymentStateTransitionService service = new PaymentStateTransitionService();

    @Test
    void transition_rejectsTerminalOverwrite() {
        Payment payment = payment(PaymentStatus.SUCCESS);

        PaymentStateTransitionService.TransitionResult result =
                service.transition(payment, PaymentStatus.FAILED, "CALLBACK");

        assertFalse(result.accepted());
        assertFalse(result.changed());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertNull(payment.getFailureReason());
    }

    @Test
    void transition_duplicateStatusIsAcceptedNoop() {
        Payment payment = payment(PaymentStatus.PENDING);

        PaymentStateTransitionService.TransitionResult result =
                service.transition(payment, PaymentStatus.PENDING, "CALLBACK");

        assertTrue(result.accepted());
        assertFalse(result.changed());
        assertFalse(service.shouldNotifyWebhook(result));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void transition_pendingToRequiresActionShouldNotifyWebhook() {
        Payment payment = payment(PaymentStatus.PENDING);

        PaymentStateTransitionService.TransitionResult result =
                service.transition(payment, PaymentStatus.REQUIRES_ACTION, "PAYIN_INITIATE");

        assertTrue(result.accepted());
        assertTrue(result.changed());
        assertTrue(service.shouldNotifyWebhook(result));
        assertEquals(PaymentStatus.REQUIRES_ACTION, payment.getStatus());
    }

    @Test
    void transition_pendingReconciliationCanOnlyBecomeTerminal() {
        Payment payment = payment(PaymentStatus.PENDING_RECONCILIATION);

        PaymentStateTransitionService.TransitionResult result =
                service.transition(payment, PaymentStatus.PENDING, "CALLBACK");

        assertFalse(result.accepted());
        assertEquals(PaymentStatus.PENDING_RECONCILIATION, payment.getStatus());
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setPaymentId("fp_test");
        payment.setStatus(status);
        payment.setFailureReason(null);
        return payment;
    }
}
