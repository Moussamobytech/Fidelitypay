package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "routes", indexes = {
        @Index(name = "idx_operator_availability", columnList = "operator, availability"),
        @Index(name = "idx_provider", columnList = "provider"),
        @Index(name = "idx_name", columnList = "name")
})
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Exemple : SAMIRPAY_OM, PAYDUNYA_WAVE
    @Column(nullable = false, unique = true)
    private String name;

    // OM, WAVE, MOOV
    @Column(nullable = false)
    private String operator;

    // SAMIRPAY, PAYDUNYA
    @Column(nullable = false)
    private String provider;

    // Disponibilité actuelle de la route
    @Column(nullable = false)
    private boolean availability = true;

    // Coût de transaction
    @Column(nullable = false)
    private double cost = 0.0;

    // Temps moyen de réponse (ms)
    @Column(nullable = false)
    private double avgLatency = 0.0;

    // Taux d’échec (0 → 1)
    @Column(nullable = false)
    private double failureRate = 0.0;

    // Priorité manuelle (plus petit = meilleur)
    @Column(nullable = false)
    private int priority = 0;

    // Optionnel : score calculé pour tri rapide
    @Transient
    private double score;
}
