package com.Api.Fidelitypay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "merchant_provider_accounts", indexes = {
        @Index(name = "idx_merchant_provider_account_user", columnList = "merchant_user_id"),
        @Index(name = "idx_merchant_provider_account_provider", columnList = "provider_id"),
        @Index(name = "idx_merchant_provider_account_enabled", columnList = "enabled")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_provider_account", columnNames = {
                "merchant_user_id", "provider_id", "environment"
        })
})
public class MerchantProviderAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_user_id", nullable = false, length = 255)
    private String merchantUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider provider;

    @Column(nullable = false, length = 20)
    private String environment = "LIVE";

    @Column(name = "credentials_encrypted", nullable = false, columnDefinition = "TEXT")
    private String credentialsEncrypted;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void normalize() {
        this.environment = environment == null ? "LIVE" : environment.trim().toUpperCase();
    }
}
