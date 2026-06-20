package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
@Slf4j
public class PaymentStateTransitionService {

    private static final Set<PaymentStatus> TERMINAL_STATUSES = EnumSet.of(
            PaymentStatus.SUCCESS,
            PaymentStatus.FAILED,
            PaymentStatus.CANCELLED);

    public TransitionResult transition(Payment payment, PaymentStatus targetStatus, String source) {
        if (payment == null) {
            return TransitionResult.rejected(null, targetStatus, "PAYMENT_REQUIRED");
        }
        if (targetStatus == null) {
            return TransitionResult.rejected(payment.getStatus(), null, "TARGET_STATUS_REQUIRED");
        }

        PaymentStatus currentStatus = payment.getStatus();
        if (currentStatus == null) {
            apply(payment, targetStatus, source);
            return TransitionResult.changed(null, targetStatus);
        }

        if (currentStatus == targetStatus) {
            return TransitionResult.duplicate(currentStatus);
        }

        if (!isAllowed(currentStatus, targetStatus)) {
            log.warn("Rejected payment status transition payment={} source={} from={} to={}",
                    payment.getPaymentId(), source, currentStatus, targetStatus);
            return TransitionResult.rejected(currentStatus, targetStatus, "INVALID_TRANSITION");
        }

        apply(payment, targetStatus, source);
        return TransitionResult.changed(currentStatus, targetStatus);
    }

    public boolean isTerminal(PaymentStatus status) {
        return TERMINAL_STATUSES.contains(status);
    }

    public boolean shouldNotifyWebhook(TransitionResult result) {
        if (result == null || !result.changed()) {
            return false;
        }
        return isTerminal(result.currentStatus()) || result.currentStatus() == PaymentStatus.REQUIRES_ACTION;
    }

    private boolean isAllowed(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        if (isTerminal(currentStatus)) {
            return false;
        }
        return switch (currentStatus) {
            case PENDING -> targetStatus == PaymentStatus.REQUIRES_ACTION
                    || targetStatus == PaymentStatus.PENDING_RECONCILIATION
                    || isTerminal(targetStatus);
            case REQUIRES_ACTION -> targetStatus == PaymentStatus.PENDING
                    || targetStatus == PaymentStatus.PENDING_RECONCILIATION
                    || isTerminal(targetStatus);
            case PENDING_RECONCILIATION -> isTerminal(targetStatus);
            case SUCCESS, FAILED, CANCELLED -> false;
        };
    }

    private void apply(Payment payment, PaymentStatus targetStatus, String source) {
        payment.setStatus(targetStatus);
        payment.setUpdatedAt(LocalDateTime.now());
        if (targetStatus == PaymentStatus.SUCCESS) {
            payment.setFailureReason(null);
            payment.setNextActionType(null);
        } else if (targetStatus == PaymentStatus.FAILED || targetStatus == PaymentStatus.CANCELLED) {
            payment.setFailureReason(nonBlank(source, "PAYMENT_STATE") + "_" + targetStatus.name());
            payment.setNextActionType(null);
        } else if (targetStatus == PaymentStatus.PENDING) {
            payment.setFailureReason(null);
            payment.setNextActionType(null);
        }
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record TransitionResult(
            boolean accepted,
            boolean changed,
            PaymentStatus previousStatus,
            PaymentStatus currentStatus,
            String reason) {

        static TransitionResult changed(PaymentStatus previousStatus, PaymentStatus currentStatus) {
            return new TransitionResult(true, true, previousStatus, currentStatus, null);
        }

        static TransitionResult duplicate(PaymentStatus status) {
            return new TransitionResult(true, false, status, status, "DUPLICATE_STATUS");
        }

        static TransitionResult rejected(PaymentStatus previousStatus, PaymentStatus targetStatus, String reason) {
            return new TransitionResult(false, false, previousStatus, targetStatus, reason);
        }
    }
}
