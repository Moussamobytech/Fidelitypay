package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KkiapayResponseDTO {

    private String status;
    private String transactionId;
    private String providerCommonName;
    private String reference;

    // En cas d'erreur ou d'URL de paiement
    private String url;
    private String message;

    // Wave specific
    private String wave_launch_url;
    private String when_created;
    private String when_expires;
    private Double fees;
    private String id;
}
