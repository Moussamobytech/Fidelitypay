package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KkiapayInvoice {
    private double totalAmount;
    private String description;
    // Add other fields if needed, e.g., customer info, tax, items
}
