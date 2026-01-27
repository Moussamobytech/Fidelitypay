package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.model.Payment;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Payment payment;

    @JsonProperty("routeAvailable")
    private boolean routeAvailable;

    @JsonProperty("route")
    private String routeName;

    @JsonProperty("provider")
    private String routeProvider;

    @JsonProperty("latence_moyenne")
    private double routeLatency;
}
