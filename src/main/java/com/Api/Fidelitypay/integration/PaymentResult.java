package com.Api.Fidelitypay.integration;

/**
 * Lightweight DTO that represents the outcome of a provider payment initiation call.
 */
public class PaymentResult {

    private boolean success;
    private String providerId;
    private String paymentUrl;
    private String rawResponse;
    private double responseTimeMs;

    public PaymentResult() {
        this.success = true;
    }

    public PaymentResult(boolean success) {
        this.success = success;
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