package com.Api.Fidelitypay.integration.paydunya.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayDunyaRequest {
    private PayDunyaInvoice invoice;
    private String store;
}
