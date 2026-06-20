package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KkiapayWaveRequestDTO {
    private long amount;
    private String email;
    private String country;
    private String name;
    private String callback;
    private Object stateData;
    private String partnerId;
    private String reason;
    private String success_url;
    private String error_url;
    /**
     * Required by the Kkiapay Wave API endpoint (/api/v1/payments/partner/wave).
     * Must be set to the merchant's public API key.
     */
    private String token;
}
