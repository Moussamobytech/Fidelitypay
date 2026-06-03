package com.Api.Fidelitypay.service.failure;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.FailureReason;
import com.Api.Fidelitypay.enums.FailureStage;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.integration.PaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentFailureClassifierTest {

    private PaymentFailureClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new PaymentFailureClassifier();
    }

    @Test
    void classifyProviderResult_mapsKnownErrorType() {
        PaymentResult result = new PaymentResult(false);
        result.setErrorType(ErrorType.TIMEOUT);
        result.setRawResponse("unexpected body");

        PaymentFailure failure = classifier.classifyProviderResult(result, FailureStage.PROVIDER_INIT);

        assertEquals(FailureReason.TIMEOUT, failure.failureReason());
        assertEquals(ErrorType.TIMEOUT, failure.errorType());
        assertEquals(FailureStage.PROVIDER_INIT, failure.failureStage());
    }

    @Test
    void classifyProviderResult_detectsInsufficientFundsFromMessage() {
        PaymentResult result = new PaymentResult(false);
        result.setRawResponse("Erreur: Solde insuffisant pour la transaction");

        PaymentFailure failure = classifier.classifyProviderResult(result, FailureStage.PROVIDER_INIT);

        assertEquals(FailureReason.INSUFFICIENT_FUNDS, failure.failureReason());
    }

    @Test
    void classifyProviderResult_defaultsToUnknownForAmbiguousFailure() {
        PaymentResult result = new PaymentResult(false);
        result.setRawResponse("something went wrong");

        PaymentFailure failure = classifier.classifyProviderResult(result, FailureStage.PROVIDER_INIT);

        assertEquals(FailureReason.UNKNOWN, failure.failureReason());
        assertEquals(ErrorType.UNKNOWN, failure.errorType());
    }

    @Test
    void classifyCallback_mapsProviderFailure() {
        PaymentFailure failure = classifier.classifyCallback(PaymentStatus.FAILED);

        assertEquals(FailureReason.PROVIDER_REPORTED_FAILURE, failure.failureReason());
        assertEquals(FailureStage.PROVIDER_CALLBACK, failure.failureStage());
    }
}
