package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.enums.ErrorType;

/**
 * Lightweight DTO that represents the outcome of a provider payment initiation
 * call.
 */
public class PaymentResult {

    private boolean success;
    private String providerId;
    private String paymentUrl;
    private String rawResponse;
    private double responseTimeMs;
    private ErrorType errorType;
    private String actualOperator;
    private boolean providerTransactionCreated;
    private boolean requiresAction;
    private String nextActionType;

    public PaymentResult() {
        this.success = true;
    }

    public PaymentResult(boolean success) {
        this.success = success;
    }

    public String getActualOperator() {
        return actualOperator;
    }

    public void setActualOperator(String actualOperator) {
        this.actualOperator = actualOperator;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public boolean isProviderTransactionCreated() {
        return providerTransactionCreated;
    }

    public void setProviderTransactionCreated(boolean providerTransactionCreated) {
        this.providerTransactionCreated = providerTransactionCreated;
    }

    public boolean isRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public String getNextActionType() {
        return nextActionType;
    }

    public void setNextActionType(String nextActionType) {
        this.nextActionType = nextActionType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public double getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(double responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    @Override
    public String toString() {
        return "PaymentResult{" +
                "success=" + success +
                ", providerId='" + providerId + '\'' +
                ", paymentUrl='" + paymentUrl + '\'' +
                ", responseTimeMs=" + responseTimeMs +
                '}';
    }
}
