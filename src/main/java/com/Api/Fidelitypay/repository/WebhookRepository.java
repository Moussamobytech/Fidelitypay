package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Webhook management
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, String> {

    /**
     * Find all webhooks for a user
     */
    List<Webhook> findByUserId(String userId);

    /**
     * Find active webhooks for a user
     */
    List<Webhook> findByUserIdAndIsActive(String userId, boolean isActive);

    /**
     * Find webhooks by event type for a user
     */
    List<Webhook> findByUserIdAndEvent(String userId, String event);

    /**
     * Find active webhooks for a specific event
     */
    List<Webhook> findByUserIdAndEventAndIsActive(String userId, String event, boolean isActive);

    /**
     * Find webhook by ID and user (for authorization)
     */
    Optional<Webhook> findByIdAndUserId(String id, String userId);

    /**
     * Check if a webhook URL already exists for a user
     */
    boolean existsByUserIdAndUrlAndEvent(String userId, String url, String event);

    /**
     * Count active webhooks for a user
     */
    long countByUserIdAndIsActive(String userId, boolean isActive);
}
