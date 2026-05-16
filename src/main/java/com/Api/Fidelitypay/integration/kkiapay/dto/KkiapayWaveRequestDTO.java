package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KkiapayWaveRequestDTO {
    private double amount;
    private String email;
    private String country;
    private String name;
    private String callback;
    private Object stateData;
    private String partnerId;
    private String reason;
    private String success_url;
    private String error_url;
}
