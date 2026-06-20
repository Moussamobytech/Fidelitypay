package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KkiapayRequestDTO {
    private long amount;
    private String phoneNumber;
    private String country;
    private String firstname;
    private String lastname;
    private String callback;
    private Object stateData;
    private String partnerId;
    private String reason;
    private String email;
    private String name;
    private String success_url;
    private String error_url;
    private String operator;
    private String payment_method;
}
