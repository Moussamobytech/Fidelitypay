package com.Api.Fidelitypay.model;

import com.Api.Fidelitypay.enums.ApiRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for tracking API request activity/logs
 * Used for monitoring, metrics, and developer activity feed
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "api_request_logs", indexes = {
        @Index(name = "idx_api_log_api_key_id", columnList = "apiKeyId"),
        @Index(name = "idx_api_log_user_id", columnList = "userId"),
        @Index(name = "idx_api_log_status", columnList = "status"),
        @Index(name = "idx_api_log_created_at", columnList = "createdAt"),
        @Index(name = "idx_api_log_status_code", columnList = "statusCode")
})
public class ApiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * API Key used for this request
     */
    @Column(nullable = false, length = 255)
    private String apiKeyId;

    /**
     * User ID who owns the API key
     */
    @Column(nullable = false, length = 255)
    private String userId;

    /**
     * HTTP method (GET, POST, etc.)
     */
    @Column(nullable = false, length = 10)
    private String method;

    /**
     * Endpoint path (e.g., /api/v1/payments/initiate)
     */
    @Column(nullable = false, length = 500)
    private String endpoint;

    /**
     * HTTP status code (200, 400, 500, etc.)
     */
    @Column(nullable = false)
    private int statusCode;

    /**
     * Request status (SUCCESS, ERROR, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiRequestStatus status;

    /**
     * Client IP address
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * User agent string
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * Response latency in milliseconds
     */
    @Column(nullable = false)
    private long latencyMs;

    /**
     * Error message if request failed
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Request timestamp
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Additional metadata (can store request/response details)
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
