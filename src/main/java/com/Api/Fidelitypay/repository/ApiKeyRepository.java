package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for API Key management
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    /**
     * Find all API keys for a specific user
     */
    List<ApiKey> findByUserId(String userId);

    /**
     * Find active API keys for a user
     */
    List<ApiKey> findByUserIdAndIsActive(String userId, boolean isActive);

    /**
     * Find API keys by environment (sandbox/live) for a user
     */
    List<ApiKey> findByUserIdAndEnvironment(String userId, String environment);

    /**
     * Find by public key
     */
    Optional<ApiKey> findByPublicKey(String publicKey);

    /**
     * Find active API key by public key
     */
    Optional<ApiKey> findByPublicKeyAndIsActive(String publicKey, boolean isActive);

    /**
     * Check if public key already exists
     */
    boolean existsByPublicKey(String publicKey);

    /**
     * Count active keys for a user
     */
    long countByUserIdAndIsActive(String userId, boolean isActive);

    /**
     * Find expired keys
     */
    @Query("SELECT a FROM ApiKey a WHERE a.expiresAt IS NOT NULL AND a.expiresAt < :now AND a.isActive = true")
    List<ApiKey> findExpiredKeys(LocalDateTime now);

    /**
     * Find keys not used for a certain period
     */
    @Query("SELECT a FROM ApiKey a WHERE a.lastUsedAt IS NOT NULL AND a.lastUsedAt < :since")
    List<ApiKey> findUnusedKeysSince(LocalDateTime since);
}
