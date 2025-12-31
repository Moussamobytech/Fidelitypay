package com.Api.Fidelitypay.model;

import com.Api.Fidelitypay.Enum.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_payment_id", columnList = "paymentId"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_operator", columnList = "operator")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String paymentId;

    /** Opérateur choisi par l’utilisateur (OM, Wave…) */
    @Column(nullable = false)
    private String operator;

    /** Agrégateur réellement utilisé (SamirPay, PayDunya) */
    @Column
    private String provider;

    /** Route technique utilisée */
    @Column
    private String routeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** Montant payé par le client */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** Coût réel de la route */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false, length = 3)
    private String currency;

    /** Pays du paiement */
    @Column(nullable = false, length = 50)
    private String country;

    /** Tentatives (fallback / retry) */
    private int attemptCount;

    /** Infos fournisseur */
    private String providerPaymentId;

    @Column(columnDefinition = "TEXT")
    private String providerResponse;

    private String paymentUrl;

    private Long providerResponseTimeMs;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
