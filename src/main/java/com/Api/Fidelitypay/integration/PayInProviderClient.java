package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.model.Payment;

public interface PayInProviderClient {
    String getProviderName();

    PaymentResult initiatePayIn(PayInProviderRequest request);

    default PaymentResult validateAction(Payment payment, String actionType, String value) {
        PaymentResult result = new PaymentResult(false);
        result.setRawResponse("Unsupported action: " + actionType);
        return result;
    }

    default PaymentStatus checkStatus(String providerPaymentId) {
        return PaymentStatus.PENDING_RECONCILIATION;
    }

    boolean isAvailable();
}
