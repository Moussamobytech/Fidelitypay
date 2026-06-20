package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Singleton table (always id = 1) storing the global routing fallback configuration.
 */
@Entity
@Data
@Table(name = "routing_fallback_config")
public class RoutingFallbackConfig {

    @Id
    private Long id = 1L; // Singleton row

    /** Enable automatic failover to the next provider after a failed attempt. */
    @Column(nullable = false)
    private boolean autoFallback = true;

    /** Enable intelligent retry on 5xx errors. */
    @Column(nullable = false)
    private boolean smartRetry = true;

    /** Number of retry attempts when smartRetry is enabled (1-10). */
    @Column(nullable = false)
    private int retryMaxAttempts = 3;

    /** Timeout in seconds before switching to fallback provider (1-60). */
    @Column(nullable = false)
    private int criticalTimeoutSeconds = 10;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // Clamp values to safe ranges
        this.retryMaxAttempts = Math.max(1, Math.min(10, retryMaxAttempts));
        this.criticalTimeoutSeconds = Math.max(1, Math.min(60, criticalTimeoutSeconds));
    }
}
