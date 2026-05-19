package com.Api.Fidelitypay.integration.paydunya.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayDunyaCustomerDTO {
    private String name;
    private String email;
    private String phone;
}
