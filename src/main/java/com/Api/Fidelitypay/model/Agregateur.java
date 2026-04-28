package com.Api.Fidelitypay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(nullable = false, length = 100)
    private String nompays;

    @Column(nullable = false, length = 100)
    private String nomOperateur;
}
