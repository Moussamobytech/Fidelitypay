package com.Api.Fidelitypay.service.failure;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.FailureReason;
import com.Api.Fidelitypay.enums.FailureStage;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.PaymentResult;
import com.Api.Fidelitypay.model.Payment;
import org.springframework.stereotype.Component;

/**
 * Classification centralisée des échecs de paiement (v1 pragmatique).
 * <p>
 * Seuls les motifs évidents sont reconnus ; tout le reste est {@link FailureReason#UNKNOWN}.
 */
@Component
public class PaymentFailureClassifier {

    public PaymentFailure known(FailureReason reason, FailureStage stage) {
        return new PaymentFailure(reason, errorTypeFor(reason), stage);
    }

    public PaymentFailure classifyProviderResult(PaymentResult result, FailureStage stage) {
        if (result == null) {
            return PaymentFailure.unknown(stage);
        }

        if (result.getErrorType() != null) {
            FailureReason fromType = fromErrorType(result.getErrorType());
            if (fromType != FailureReason.UNKNOWN) {
                return new PaymentFailure(fromType, result.getErrorType(), stage);
            }
        }

        FailureReason fromMessage = parseObviousMessage(result.getRawResponse());
        if (fromMessage != FailureReason.UNKNOWN) {
            return new PaymentFailure(fromMessage, errorTypeFor(fromMessage), stage);
        }

        ErrorType errorType = result.getErrorType() != null ? result.getErrorType() : ErrorType.UNKNOWN;
        return new PaymentFailure(FailureReason.UNKNOWN, errorType, stage);
    }

    public PaymentFailure classifyCallback(PaymentStatus status) {
        if (status == PaymentStatus.CANCELLED) {
            return known(FailureReason.CANCELLED_BY_USER, FailureStage.PROVIDER_CALLBACK);
        }
        if (status == PaymentStatus.FAILED) {
            return known(FailureReason.PROVIDER_REPORTED_FAILURE, FailureStage.PROVIDER_CALLBACK);
        }
        return PaymentFailure.unknown(FailureStage.PROVIDER_CALLBACK);
    }

    public void apply(Payment payment, PaymentFailure failure) {
        payment.setFailureReason(failure.reasonCode());
        payment.setErrorType(failure.errorType());
        payment.setFailureStage(failure.failureStage());
    }

    public void clear(Payment payment) {
        payment.setFailureReason(null);
        payment.setErrorType(null);
        payment.setFailureStage(null);
    }

    private FailureReason fromErrorType(ErrorType errorType) {
        return switch (errorType) {
            case AUTHENTICATION -> FailureReason.AUTHENTICATION_FAILED;
            case TIMEOUT -> FailureReason.TIMEOUT;
            case NETWORK -> FailureReason.NETWORK_ERROR;
            case PROVIDER_DOWN -> FailureReason.PROVIDER_DOWN;
            case BAD_REQUEST -> FailureReason.BAD_REQUEST;
            case INTERNAL_ERROR -> FailureReason.INTERNAL_ERROR;
            case UNKNOWN -> FailureReason.UNKNOWN;
        };
    }

    private FailureReason parseObviousMessage(String message) {
        if (message == null || message.isBlank()) {
            return FailureReason.UNKNOWN;
        }
        String upper = message.toUpperCase();
        if (containsAny(upper, "SOLDE", "INSUFFICIENT", "FUNDS", "FUND")) {
            return FailureReason.INSUFFICIENT_FUNDS;
        }
        if (containsAny(upper, "INVALID PHONE", "PHONE NUMBER", "NUMERO", "NUMÉRO")) {
            return FailureReason.INVALID_PHONE_NUMBER;
        }
        if (containsAny(upper, "PAYMENT CHANNEL", "INVALID_OPERATOR", "NOT A VALID PAYMENT CHANNEL")) {
            return FailureReason.INVALID_OPERATOR;
        }
        if (containsAny(upper, "CANCEL", "ANNUL")) {
            return FailureReason.CANCELLED_BY_USER;
        }
        if (containsAny(upper, "TIMEOUT", "TIME_OUT", "TIMED OUT")) {
            return FailureReason.TIMEOUT;
        }
        if (containsAny(upper, "CONNECTION", "NETWORK", "DNS", "UNREACHABLE")) {
            return FailureReason.NETWORK_ERROR;
        }
        if (containsAny(upper, "503", "UNAVAILABLE", "SERVICE DOWN", "PROVIDER DOWN")) {
            return FailureReason.PROVIDER_DOWN;
        }
        if (containsAny(upper, "401", "403", "AUTH", "TOKEN", "UNAUTHORIZED", "FORBIDDEN")) {
            return FailureReason.AUTHENTICATION_FAILED;
        }
        return FailureReason.UNKNOWN;
    }

    private ErrorType errorTypeFor(FailureReason reason) {
        return switch (reason) {
            case AUTHENTICATION_FAILED -> ErrorType.AUTHENTICATION;
            case TIMEOUT -> ErrorType.TIMEOUT;
            case NETWORK_ERROR -> ErrorType.NETWORK;
            case PROVIDER_DOWN -> ErrorType.PROVIDER_DOWN;
            case BAD_REQUEST, INVALID_OPERATOR, INVALID_PHONE_NUMBER -> ErrorType.BAD_REQUEST;
            case INTERNAL_ERROR, OTP_VALIDATION_FAILED -> ErrorType.INTERNAL_ERROR;
            case INSUFFICIENT_FUNDS, CANCELLED_BY_USER, PROVIDER_REPORTED_FAILURE,
                    PROVIDER_RESULT_UNKNOWN, NO_PROVIDER_AVAILABLE_FOR_COUNTRY,
                    NO_PROVIDER_AVAILABLE_FOR_ENVIRONMENT, UNSUPPORTED_PAYIN_CAPABILITY,
                    NO_FALLBACK_PROVIDER_AVAILABLE -> ErrorType.UNKNOWN;
            case UNKNOWN -> ErrorType.UNKNOWN;
        };
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
