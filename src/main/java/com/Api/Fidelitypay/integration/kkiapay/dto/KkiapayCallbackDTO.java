package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KkiapayCallbackDTO {
    private String transactionId;
    private boolean isPaymentSucces;
    private String account;
    private String label;
    private String method;
    private double amount;
    private double fees;
    private String partnerId;
    private Object stateData;
    private String performedAt;
    private String event;
}
