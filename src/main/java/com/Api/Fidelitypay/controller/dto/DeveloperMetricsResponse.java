package com.Api.Fidelitypay.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for developer metrics/statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperMetricsResponse {

    /**
     * Total number of API requests in the period
     */
    private long totalRequests;

    /**
     * Success rate percentage (0-100)
     */
    private double successRate;

    /**
     * Average response latency in milliseconds
     */
    private double avgLatency;

    /**
     * Error rate percentage (0-100)
     */
    private double errorRate;

    /**
     * Total number of successful requests
     */
    private long successfulRequests;

    /**
     * Total number of failed requests
     */
    private long failedRequests;

    /**
     * Hourly breakdown of requests (last 24h)
     */
    private List<HourlyMetric> hourlyBreakdown;

    /**
     * Top endpoints by usage
     */
    private List<EndpointMetric> topEndpoints;

    /**
     * Current period (e.g., "last_24h", "last_7d")
     */
    private String period;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyMetric {
        private String hour;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointMetric {
        private String endpoint;
        private long count;
    }
}
