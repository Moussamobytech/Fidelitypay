package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import lombok.Data;

@Data
public class PaymentProviderRouteRequest {
    private Long providerId;
    private PaymentDirection direction = PaymentDirection.PAYIN;
    private String country;
    private String operator;
    private PaymentFlowType flowType;
    private boolean liveEnabled = true;
    private boolean sandboxEnabled = true;
    private String providerChannel;
    private boolean enabled = true;
    private int priority = 100;
    private double cost = 0.0;
}
