package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRouteSettingResponse {
    private Long routeId;
    private String provider;
    private PaymentDirection direction;
    private String country;
    private String operator;
    private PaymentFlowType flowType;
    private String environment;
    private String providerChannel;
    private int priority;
    private boolean platformEnabled;
    private boolean observedUp;
    private Boolean merchantEnabled;
    private boolean effectiveEnabled;
}
