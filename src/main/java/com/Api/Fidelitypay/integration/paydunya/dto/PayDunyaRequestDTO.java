package com.Api.Fidelitypay.integration.paydunya.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayDunyaRequestDTO {
    private PayDunyaInvoiceDTO invoice;
    private PayDunyaStoreDTO store;
    private Object custom_data;
    private PayDunyaActionsDTO actions;

    public PayDunyaRequestDTO(PayDunyaInvoiceDTO invoice, PayDunyaStoreDTO store) {
        this.invoice = invoice;
        this.store = store;
    }
}
