package com.Api.Fidelitypay.integration;

import com.Api.Fidelitypay.enums.PaymentFlowType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayInProviderRequest {
    private ProviderCredentials credentials;
    private String environment;
    private String paymentId;
    private long amount;
    private String country;
    private String operator;
    private String providerChannel;
    private PaymentFlowType flowType;
    private String phone;
    private String firstname;
    private String lastname;
    private String email;
    private String callbackUrl;
    private String returnUrl;
    private String cancelUrl;
}
