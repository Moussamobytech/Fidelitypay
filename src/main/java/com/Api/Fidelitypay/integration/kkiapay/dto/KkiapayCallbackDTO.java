package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.Data;

@Data
public class KkiapayCallbackDTO {
    private String transactionId;
    private boolean isPaymentSucces;
    private String account;
    private String label;
    private String method; // MOBILE_MONEY | CARD | WALLET
    private double amount;
    private double fees;
    private String partnerId;
    private Object stateData;
    private String performedAt;
    private String event; // transaction.success | transaction.failed
}
