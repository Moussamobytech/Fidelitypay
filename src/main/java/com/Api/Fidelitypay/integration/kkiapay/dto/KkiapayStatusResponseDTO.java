package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.Data;

@Data
public class KkiapayStatusResponseDTO {
    private double amount;
    private double fees;
    private double income;
    private String transactionId;
    private String country;
    private String source_common_name;
    private String source; // Mobile Money | CARD | WALLET
    private String type; // DEBIT ou CREDIT
    private String status; // SUCCESS | PENDING | FAILED
    private Object state;
    private String partnerId;
    private String performed_at;
    private String failureCode;
    private String failureMessage;
    private String feeSupportedBy; // customer | merchant
    private String reason;
}
