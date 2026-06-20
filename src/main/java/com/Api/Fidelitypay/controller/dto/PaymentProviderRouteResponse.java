package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentProviderRouteResponse {
    private Long routeId;
    private Long providerId;
    private String providerCode;
    private String providerDisplayName;
    private PaymentDirection direction;
    private String country;
    private String operator;
    private PaymentFlowType flowType;
    private boolean liveEnabled;
    private boolean sandboxEnabled;
    private String providerChannel;
    private int priority;
    private Integer merchantPriority;
    private int effectivePriority;
    private double cost;
    private double avgLatency;
    private double failureRate;
    private Double selectionScore;
    private boolean platformEnabled;
    private Boolean merchantEnabled;
    private boolean effectiveEnabled;
}
