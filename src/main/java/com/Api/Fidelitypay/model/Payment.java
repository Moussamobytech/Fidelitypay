package com.Api.Fidelitypay.model;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.PaymentStatus;
import com.Api.Fidelitypay.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
        @Index(name = "idx_payment_operator", columnList = "operator"),
        @Index(name = "idx_payment_fallback", columnList = "usedFallback") // Nouvel index
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false, unique = true, length = 50)
    private String paymentId;

    @Column(length = 255)
    private String idempotencyKey;

    @Column(length = 255)
    private String apiKeyId;

    /** Origin of the payment initiation, used for integration-readiness tracking. */
    @Column(length = 20)
    private String initiationSource;

    /** Opérateur choisi par l'utilisateur (OM, Wave…) */
    @Column(nullable = false)
    private String operator;

    /** Agrégateur réellement utilisé (SamirPay, PayDunya) */
    @Column
    private String provider;

    @Column(length = 40)
    private String flowType;

    @Column(length = 100)
    private String providerChannel;

    @Column(name = "merchant_provider_account_id")
    private Long merchantProviderAccountId;

    /** Route technique utilisée */
    @Column
    @JsonProperty("route")
    private String routeName;

    /** Santé de la route au moment du paiement */
    @Column(length = 20)
    @JsonProperty("sante")
    private String routeHealth;

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

    @Column(length = 1000)
    private String returnUrl;

    @Column(length = 1000)
    private String cancelUrl;

    @Column(length = 1000)
    private String callbackUrl;

    @Column(length = 50)
    private String customerPhone;

    @Column(length = 120)
    private String customerFirstname;

    @Column(length = 120)
    private String customerLastname;

    @Column(length = 255)
    private String customerEmail;

    @Column(length = 80)
    private String nextActionType;

    @Column(nullable = false)
    private boolean usedFallback = false;

    /** 🔥 NOUVEAU : Raison du fallback */
    @Column(name = "fallback_reason", length = 100)
    private String fallbackReason;

    @JsonProperty("latence")
    private Long providerResponseTimeMs;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 🔥 Exposer l'ID de l'utilisateur pour le frontend
    @JsonProperty("userId")
    public String getUserId() {
        return user != null ? user.getId() : null;
    }

    @JsonProperty("appName")
    public String getAppName() {
        return user != null ? user.getApplicationName() : "Inconnu";
    }
}
