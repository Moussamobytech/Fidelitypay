package com.Api.Fidelitypay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "agregateurs")
public class Agregateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nomA;

    @Column(nullable = false, length = 255)
    private String cleApblic;

    @Column(nullable = false, length = 255)
    private String cleApr;

    @Column(nullable = false, length = 255)
    private String cleAtoken;

    @Column(nullable = true, length = 255)
    private String cleAmaster;

    @Column(nullable = true, length = 255)
    private String baseUrl;

    // Old fields kept as nullable to avoid DB constraint errors during migration
    @Column(nullable = true)
    private String nompays;

    @Column(nullable = true)
    private String nomOperateur;

    @OneToMany(mappedBy = "agregateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private java.util.List<CountryConfig> countryConfigs = new java.util.ArrayList<>();
}
