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
    private int amount;
    @com.fasterxml.jackson.annotation.JsonProperty("phone_number")
    private String phoneNumber;
    private String country;
    private String firstname;
    private String lastname;
    private String email;
    private String name;
    private String callback;
    private Object stateData;
    private String partnerId;
    private String reason;
    private String success_url;
    private String error_url;
    private String operator;
    @com.fasterxml.jackson.annotation.JsonProperty("payment_method")
    private String payment_method;
    @com.fasterxml.jackson.annotation.JsonProperty("method")
    private String directMethod;
}
