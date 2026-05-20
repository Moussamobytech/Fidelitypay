package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.model.Payment;

public interface PayInProviderClient {
    String getProviderName();

    PaymentResult initiatePayIn(PayInProviderRequest request);

    default PaymentResult validateAction(Payment payment, String actionType, String value) {
        return validateAction(payment, actionType, value, null);
    }

    default PaymentResult validateAction(Payment payment, String actionType, String value, ProviderCredentials credentials) {
        PaymentResult result = new PaymentResult(false);
        result.setRawResponse("Unsupported action: " + actionType);
        return result;
    }

    default PaymentStatus checkStatus(String providerPaymentId) {
        return checkStatus(providerPaymentId, null);
    }

    default PaymentStatus checkStatus(String providerPaymentId, ProviderCredentials credentials) {
        return PaymentStatus.PENDING_RECONCILIATION;
    }

    boolean isAvailable();
}
