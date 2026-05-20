package com.Api.Fidelitypay.model;

import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment_providers", indexes = {
        @Index(name = "idx_payment_provider_code", columnList = "code"),
        @Index(name = "idx_payment_provider_status", columnList = "status")
})
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentProviderStatus status = PaymentProviderStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String credentialSchema;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void normalize() {
        this.code = normalize(code);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
