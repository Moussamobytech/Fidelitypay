package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.Data;

@Data
public class KkiapayWaveResponseDTO {
    private String transactionId;
    private String status;
    private String wave_launch_url;
    private String when_created;
    private String when_expires;
    private double amount;
    private double fees;
    private String id;
}
