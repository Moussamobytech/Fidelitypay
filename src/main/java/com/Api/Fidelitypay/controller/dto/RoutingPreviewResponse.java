package com.Api.Fidelitypay.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RoutingPreviewResponse {
    private String country;
    private String operator;
    private String environment;
    private String scoringVersion;
    private LocalDateTime evaluatedAt;
    private Candidate selected;
    private List<Candidate> candidates;

    @Data
    @Builder
    public static class Candidate {
        private int rank;
        private Long routeId;
        private String provider;
        private String flowType;
        private int effectivePriority;
        private double cost;
        private double avgLatencyMs;
        private double initiationFailureRate;
        private int sampleCount;
        private boolean sufficientSamples;
        private double score;
    }
}
