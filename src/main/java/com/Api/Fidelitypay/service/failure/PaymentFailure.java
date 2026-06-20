package com.Api.Fidelitypay.service.failure;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.FailureReason;
import com.Api.Fidelitypay.enums.FailureStage;

/**
 * Classification structurée d'un échec de paiement.
 */
public record PaymentFailure(
        FailureReason failureReason,
        ErrorType errorType,
        FailureStage failureStage) {

    public static PaymentFailure unknown(FailureStage stage) {
        return new PaymentFailure(FailureReason.UNKNOWN, ErrorType.UNKNOWN, stage);
    }

    /** Code exposé en API / webhook (nom de l'enum). */
    public String reasonCode() {
        return failureReason.name();
    }
}
