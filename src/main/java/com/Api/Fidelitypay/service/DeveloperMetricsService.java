package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.ApiActivityResponse;
import com.Api.Fidelitypay.controller.dto.DeveloperMetricsResponse;
import com.Api.Fidelitypay.enums.ApiRequestStatus;
import com.Api.Fidelitypay.model.ApiRequestLog;
import com.Api.Fidelitypay.repository.ApiRequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for calculating and retrieving developer metrics and activity
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeveloperMetricsService {

    private final ApiRequestLogRepository logRepository;

    /**
     * Get comprehensive metrics for a user over a time period
     */
    public DeveloperMetricsResponse getMetrics(String userId, String period) {
        LocalDateTime since = calculateSinceDate(period);

        // Calculate basic metrics
        long totalRequests = logRepository.countRequestsSince(userId, since);
        long successfulRequests = logRepository.countSuccessfulRequestsSince(userId, since);
        long errorRequests = logRepository.countErrorRequestsSince(userId, since);

        // Calculate averages
        Double avgLatency = logRepository.calculateAverageLatencySince(userId, since);
        if (avgLatency == null) {
            avgLatency = 0.0;
        }

        // Calculate rates
        double successRate = totalRequests > 0 ? (successfulRequests * 100.0 / totalRequests) : 0.0;
        double errorRate = totalRequests > 0 ? (errorRequests * 100.0 / totalRequests) : 0.0;

        // Get hourly breakdown
        List<DeveloperMetricsResponse.HourlyMetric> hourlyBreakdown = getHourlyBreakdown(userId, since);

        // Get top endpoints
        List<DeveloperMetricsResponse.EndpointMetric> topEndpoints = getTopEndpoints(userId, since);

        return DeveloperMetricsResponse.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(errorRequests)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .errorRate(Math.round(errorRate * 100.0) / 100.0)
                .avgLatency(Math.round(avgLatency * 100.0) / 100.0)
                .hourlyBreakdown(hourlyBreakdown)
                .topEndpoints(topEndpoints)
                .period(period)
                .build();
    }

    /**
     * Get recent API activity/logs for a user
     */
    public List<ApiActivityResponse> getActivity(String userId, int limit, String filter) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        List<ApiRequestLog> logs;

        switch (filter) {
            case "errors":
                logs = logRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, ApiRequestStatus.ERROR, pageRequest);
                break;
            case "slow":
                // Consider requests over 1000ms as slow
                logs = logRepository.findSlowRequests(userId, 1000L, pageRequest);
                break;
            default: // "all"
                logs = logRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
                break;
        }

        return logs.stream()
                .map(this::toActivityResponse)
                .collect(Collectors.toList());
    }

    /**
     * Log an API request (to be called by API interceptor/filter)
     */
    public void logApiRequest(String userId, String apiKeyId, String method, String endpoint,
            int statusCode, ApiRequestStatus status, String ipAddress,
            String userAgent, long latencyMs, String errorMessage) {
        ApiRequestLog log = ApiRequestLog.builder()
                .userId(userId)
                .apiKeyId(apiKeyId)
                .method(method)
                .endpoint(endpoint)
                .statusCode(statusCode)
                .status(status)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .latencyMs(latencyMs)
                .errorMessage(errorMessage)
                .build();

        logRepository.save(log);
    }

    /**
     * Get hourly breakdown of requests
     */
    private List<DeveloperMetricsResponse.HourlyMetric> getHourlyBreakdown(String userId, LocalDateTime since) {
        List<Object[]> results = logRepository.getHourlyRequestCounts(userId, since);

        return results.stream()
                .map(row -> DeveloperMetricsResponse.HourlyMetric.builder()
                        .hour((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get top endpoints by usage
     */
    private List<DeveloperMetricsResponse.EndpointMetric> getTopEndpoints(String userId, LocalDateTime since) {
        List<Object[]> results = logRepository.getTopEndpoints(userId, since);

        return results.stream()
                .map(row -> DeveloperMetricsResponse.EndpointMetric.builder()
                        .endpoint((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Calculate the "since" date based on period string
     */
    private LocalDateTime calculateSinceDate(String period) {
        return switch (period) {
            case "last_hour" -> LocalDateTime.now().minusHours(1);
            case "last_24h" -> LocalDateTime.now().minusHours(24);
            case "last_7d" -> LocalDateTime.now().minusDays(7);
            case "last_30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusHours(24); // default to 24h
        };
    }

    /**
     * Convert ApiRequestLog to ApiActivityResponse DTO
     */
    private ApiActivityResponse toActivityResponse(ApiRequestLog log) {
        return ApiActivityResponse.builder()
                .id(log.getId())
                .method(log.getMethod())
                .endpoint(log.getEndpoint())
                .statusCode(log.getStatusCode())
                .status(log.getStatus().name())
                .ipAddress(log.getIpAddress())
                .latencyMs(log.getLatencyMs())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * Clean up old logs (can be scheduled)
     */
    public void cleanupOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        logRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("🧹 Cleaned up API logs older than {} days", daysToKeep);
    }
}
