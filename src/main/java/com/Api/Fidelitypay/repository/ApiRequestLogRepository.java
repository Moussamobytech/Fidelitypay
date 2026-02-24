package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.enums.ApiRequestStatus;
import com.Api.Fidelitypay.model.ApiRequestLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for API request logging and metrics
 */
@Repository
public interface ApiRequestLogRepository extends JpaRepository<ApiRequestLog, Long> {

    /**
     * Find recent logs for a user
     */
    List<ApiRequestLog> findByUserIdOrderByCreatedAtDesc(String userId, PageRequest pageRequest);

    /**
     * Find logs by user and status
     */
    List<ApiRequestLog> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, ApiRequestStatus status,
            PageRequest pageRequest);

    /**
     * Find slow requests (above a latency threshold)
     */
    @Query("SELECT a FROM ApiRequestLog a WHERE a.userId = :userId AND a.latencyMs > :minLatency ORDER BY a.createdAt DESC")
    List<ApiRequestLog> findSlowRequests(@Param("userId") String userId, @Param("minLatency") long minLatency,
            PageRequest pageRequest);

    /**
     * Count total requests for a user in a time period
     */
    @Query("SELECT COUNT(a) FROM ApiRequestLog a WHERE a.userId = :userId AND a.createdAt >= :since")
    long countRequestsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Count successful requests for a user in a time period
     */
    @Query("SELECT COUNT(a) FROM ApiRequestLog a WHERE a.userId = :userId AND a.status = 'SUCCESS' AND a.createdAt >= :since")
    long countSuccessfulRequestsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Count error requests for a user in a time period
     */
    @Query("SELECT COUNT(a) FROM ApiRequestLog a WHERE a.userId = :userId AND a.statusCode >= 400 AND a.createdAt >= :since")
    long countErrorRequestsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Calculate average latency for a user in a time period
     */
    @Query("SELECT AVG(a.latencyMs) FROM ApiRequestLog a WHERE a.userId = :userId AND a.createdAt >= :since")
    Double calculateAverageLatencySince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Find requests by user in a time period
     */
    @Query("SELECT a FROM ApiRequestLog a WHERE a.userId = :userId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<ApiRequestLog> findRequestsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Get hourly request counts for the last 24 hours
     */
    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d %H:00:00') as hour, COUNT(*) as count " +
            "FROM api_request_logs " +
            "WHERE user_id = :userId AND created_at >= :since " +
            "GROUP BY hour " +
            "ORDER BY hour", nativeQuery = true)
    List<Object[]> getHourlyRequestCounts(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Get endpoint usage statistics
     */
    @Query(value = "SELECT endpoint, COUNT(*) as count " +
            "FROM api_request_logs " +
            "WHERE user_id = :userId AND created_at >= :since " +
            "GROUP BY endpoint " +
            "ORDER BY count DESC " +
            "LIMIT 10", nativeQuery = true)
    List<Object[]> getTopEndpoints(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Delete old logs (for cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
}
