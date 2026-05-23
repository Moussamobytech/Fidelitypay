package com.Api.Fidelitypay.model;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.enums.PaymentFlowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment_provider_routes", indexes = {
        @Index(name = "idx_provider_route_lookup", columnList = "direction,country,operator,environment,enabled"),
        @Index(name = "idx_provider_route_priority", columnList = "direction,country,operator,priority"),
        @Index(name = "idx_provider_route_provider", columnList = "provider_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_provider_route", columnNames = {
                "provider_id", "direction", "country", "operator", "flow_type", "environment"
        })
})
public class PaymentProviderRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentDirection direction = PaymentDirection.PAYIN;

    @Column(nullable = false, length = 3)
    private String country;

    @Column(nullable = false, length = 50)
    private String operator;

    @Enumerated(EnumType.STRING)
    @Column(name = "flow_type", nullable = false, length = 40)
    private PaymentFlowType flowType;

    @Column(nullable = false, length = 20)
    private String environment = "LIVE";

    @Column(nullable = false, length = 100)
    private String providerChannel;

    @Column(nullable = false)
    private boolean enabled = true;


    @Column(nullable = false)
    private int priority = 100;

    // Route price/fee input for selection. Keep static until production measurements start.
    @Column(nullable = false)
    private double cost = 0.0;

    @Column(nullable = false)
    private double avgLatency = 0.0;

    @Column(nullable = false)
    private double failureRate = 0.0;

    @Column(length = 500)
    private String lastErrorMessage;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void normalize() {
        this.country = normalize(country);
        this.operator = normalize(operator);
        this.environment = normalize(environment);
        this.updatedAt = LocalDateTime.now();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
