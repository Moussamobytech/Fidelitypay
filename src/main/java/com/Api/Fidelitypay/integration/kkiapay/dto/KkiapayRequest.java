package com.Api.Fidelitypay.integration.kkiapay.dto;

import lombok.Data;

@Data
public class KkiapayRequest {

    // Montant à payer
    private double amount;

    // Devise (XOF)
    private String currency;

    // Description / raison du paiement
    private String reason;

    // Identifiant de la transaction côté marchand
    private String transactionId;

    // Clé publique Kkiapay
    private String publicKey;

    // Nom du service ou opérateur
    private String service;
}
