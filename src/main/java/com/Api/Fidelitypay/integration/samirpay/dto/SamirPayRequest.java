package com.Api.Fidelitypay.integration.samirpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SamirPayRequest {
    private double amount;
    private String currency; // e.g. "XOF"
    private String operator; // e.g. "ORANGE_MONEY"
    private String description;
    private String customerName;
    private String customerPhone;
}
