package com.Api.Fidelitypay.integration.kkiapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KkiapayResponseDTO {

    private String status;
    private String transactionId;
    private String reference;

    // En cas d'erreur ou d'URL de paiement
    private String url;
    private String message;
}
