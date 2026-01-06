package com.Api.Fidelitypay.integration.paydunya.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PayDunyaResponseDTO {

    @JsonProperty("response_code")
    private String responseCode;

    @JsonProperty("response_text")
    private String responseText;

    private String description;
    private String token;
}
