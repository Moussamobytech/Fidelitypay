package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for managing webhook configurations
 * Allows users to register callback URLs for specific events
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "webhooks", indexes = {
        @Index(name = "idx_webhook_user_id", columnList = "userId"),
        @Index(name = "idx_webhook_event", columnList = "event"),
        @Index(name = "idx_webhook_active", columnList = "isActive")
})
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * User ID who owns this webhook
     */
    @Column(nullable = false, length = 255)
    private String userId;

    /**
     * Webhook URL to call
     */
    @Column(nullable = false, length = 1000)
    private String url;

    /**
     * Event type (e.g., payment.success, payment.failed, payment.pending)
     */
    @Column(nullable = false, length = 100)
    private String event;

    /**
     * Description/label for this webhook
     */
    @Column(length = 255)
    private String description;

    /**
     * Secret for HMAC signature verification
     */
    @Column(length = 255)
    private String secret;

    /**
     * Whether this webhook is active
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * Last time this webhook was triggered
     */
    @Column
    private LocalDateTime lastTriggeredAt;

    /**
     * Last HTTP status code received
     */
    @Column
    private Integer lastStatusCode;

    /**
     * Number of consecutive failures
     */
    @Builder.Default
    @Column(nullable = false)
    private int failureCount = 0;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column
    private LocalDateTime updatedAt;
}
