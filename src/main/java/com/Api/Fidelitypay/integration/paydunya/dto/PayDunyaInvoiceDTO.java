package com.Api.Fidelitypay.integration.paydunya.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayDunyaInvoiceDTO {

    @JsonProperty("total_amount")
    private double totalAmount;

    private String description;
    
    private java.util.List<String> channels;

    private PayDunyaCustomerDTO customer;

    public PayDunyaInvoiceDTO(double totalAmount, String description, java.util.List<String> channels) {
        this.totalAmount = totalAmount;
        this.description = description;
        this.channels = channels;
    }
}
