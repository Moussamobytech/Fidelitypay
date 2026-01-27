package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import com.Api.Fidelitypay.Enum.LogStatus;
import com.Api.Fidelitypay.Enum.ErrorType;

@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_log_payment_id", columnList = "payment_id"),
        @Index(name = "idx_log_status", columnList = "status"),
        @Index(name = "idx_log_created_at", columnList = "created_at")
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

    // ⏰ Date création
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
