package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exemple : SAMIRPAY_OM, PAYDUNYA_WAVE
    private String name;

    // OM, WAVE, MOOV
    private String operator;

    // SAMIRPAY, PAYDUNYA
    private String provider;

    private boolean availability;

    // Coût de transaction
    private double cost;

    // Temps moyen de réponse (ms)
    private double avgLatency;

    // Taux d’échec (0 → 1)
    private double failureRate;

    // Priorité manuelle (plus petit = meilleur)
    private int priority;
}
