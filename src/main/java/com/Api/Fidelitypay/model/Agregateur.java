package com.Api.Fidelitypay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "agregateurs", indexes = {
        @Index(name = "idx_agregateur_owner_user_id", columnList = "owner_user_id"),
        @Index(name = "idx_agregateur_enabled", columnList = "enabled")
})
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

    @Column(name = "owner_user_id", length = 255)
    private String ownerUserId;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "agregateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private java.util.List<CountryConfig> countryConfigs = new java.util.ArrayList<>();
}
