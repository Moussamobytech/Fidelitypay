package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.enums.ErrorType;

@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_log_payment_id", columnList = "payment_id"),
        @Index(name = "idx_log_status", columnList = "status"),
        @Index(name = "idx_log_created_at", columnList = "created_at"),
        @Index(name = "idx_log_fallback", columnList = "fallback_used"),
        @Index(name = "idx_log_provider", columnList = "provider")
})
@Getter
@Setter
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Référence paiement
    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId;

    // 🔀 Route utilisée (SAMIRPAY_OM, PAYDUNYA_WAVE…)
    @Column(name = "route_used", nullable = false, length = 100)
    private String routeUsed;

    // 🏢 Provider utilisé (KKIAPAY, PAYDUNYA…)
    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    // ⏱ Temps de réponse provider (ms)
    @Column(name = "response_time_ms")
    private Double responseTime;

    // ✅ SUCCESS / FAILED / WARNING / INFO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogStatus status;

    // 📝 Message technique
    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 255)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    // 🔄 Fallback utilisé
    @Column(name = "fallback_used")
    private Boolean fallbackUsed = false;

    // ⏰ Date création
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    // AJOUTEZ CES CONSTRUCTEURS SI NÉCESSAIRE
    public LogEntry() {
    }
    
    // Constructeur avec paramètres utiles
    public LogEntry(String paymentId, String routeUsed, String provider, Double responseTime, 
                   LogStatus status, String message) {
        this.paymentId = paymentId;
        this.routeUsed = routeUsed;
        this.provider = provider;
        this.responseTime = responseTime;
        this.status = status;
        this.message = message;
        this.fallbackUsed = false;
    }
}