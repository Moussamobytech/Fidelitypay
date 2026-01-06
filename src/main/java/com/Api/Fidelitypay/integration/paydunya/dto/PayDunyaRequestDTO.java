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
}
