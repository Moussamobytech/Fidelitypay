package com.Api.Fidelitypay.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "country_configs")
public class CountryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String countryName;

    @Column(nullable = false, length = 500)
    private String operators; // Comma-separated list of operators

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agregateur_id")
    @JsonBackReference
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Agregateur agregateur;
}
