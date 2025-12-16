package com.Api.Fidelitypay.integration.samirpay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SamirPayInvoice {
    private double totalAmount;
    private String description;
    // Add other fields if needed, e.g., customer info, tax, items
}
