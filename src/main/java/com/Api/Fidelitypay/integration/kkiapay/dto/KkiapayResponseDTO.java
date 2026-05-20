package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KkiapayResponseDTO {
    private String transactionId;
    private String providerCommonName;
    private String status;
    private String url;
    private String wave_launch_url;
}
