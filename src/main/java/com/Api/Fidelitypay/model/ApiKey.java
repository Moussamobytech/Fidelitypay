package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing API Keys for developer access
 * Stores hashed secret keys for security - never store keys in plain text!
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_key_user_id", columnList = "userId"),
        @Index(name = "idx_api_key_public_key", columnList = "publicKey"),
        @Index(name = "idx_api_key_environment", columnList = "environment"),
        @Index(name = "idx_api_key_active", columnList = "isActive")
})
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * User ID who owns this API key
     * In a real system, this would be a foreign key to users table
     */
    @Column(nullable = false, length = 255)
    private String userId;

    /**
     * Name/label for the API key (e.g., "Production Server", "Mobile App")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Public key identifier (pk_live_... or pk_sandbox_...)
     */
    @Column(nullable = false, unique = true, length = 255)
    private String publicKey;

    /**
     * Hashed secret key (sk_live_... or sk_sandbox_...)
     * NEVER store the actual secret key in plain text!
     * Use BCrypt or similar strong hashing algorithm
     */
    @Column(nullable = false, length = 255)
    private String secretKeyHash;

    /**
     * Last 4 characters of the secret key for display purposes
     */
    @Column(length = 10)
    private String secretKeyHint;

    /**
     * Environment: sandbox or live
     */
    @Column(nullable = false, length = 10)
    private String environment;

    /**
     * Last time this API key was used
     */
    @Column
    private LocalDateTime lastUsedAt;

    /**
     * IP address of the last request using this key
     */
    @Column(length = 45)
    private String lastUsedIp;

    /**
     * Whether this key is active (can be revoked)
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional expiration date for the key
     */
    @Column
    private LocalDateTime expiresAt;

}
