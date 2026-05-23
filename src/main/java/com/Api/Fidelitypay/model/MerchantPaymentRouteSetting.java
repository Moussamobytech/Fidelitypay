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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "merchant_payment_route_settings", indexes = {
        @Index(name = "idx_merchant_route_setting_user", columnList = "user_id"),
        @Index(name = "idx_merchant_route_setting_route", columnList = "payment_provider_route_id"),
        @Index(name = "idx_merchant_route_setting_enabled", columnList = "enabled")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_merchant_payment_route_setting", columnNames = {
                "user_id", "payment_provider_route_id"
        })
})
public class MerchantPaymentRouteSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_provider_route_id", nullable = false)
    private PaymentProviderRoute paymentProviderRoute;

    @Column(nullable = false)
    private boolean enabled = true;

    // Lower values are preferred for this merchant. Null keeps platform priority.
    private Integer priority;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
