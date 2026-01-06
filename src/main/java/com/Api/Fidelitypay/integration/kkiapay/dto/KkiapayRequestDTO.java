package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KkiapayRequestDTO {
    private int amount;
    private String callback;
    private String phone;
    private String reason;
    private String firstname;
    private String lastname;
    // Ajout d'email si nécessaire dans le futur
    // private String email;
}
