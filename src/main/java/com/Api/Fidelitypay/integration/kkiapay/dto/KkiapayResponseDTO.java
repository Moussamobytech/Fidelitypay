package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KkiapayResponseDTO {
    private String transactionId;
    private String internalTransactionId;
    private String providerCommonName;
    private String status;
    private String url;
    private String wave_launch_url;

    public String resolvedTransactionId() {
        return transactionId != null && !transactionId.isBlank() ? transactionId : internalTransactionId;
    }
}
