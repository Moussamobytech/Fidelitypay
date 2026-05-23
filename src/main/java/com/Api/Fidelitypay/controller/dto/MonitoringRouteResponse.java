package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.ErrorType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MonitoringRouteResponse {
    private Long id;
    private String name;
    private String provider;
    private String operator;
    private String country;
    private boolean availability;
    private double avgLatency;
    private double cost;
    private double failureRate;
    private int priority;
    private String lastErrorMessage;
    private ErrorType lastErrorType;
    private LocalDateTime updatedAt;

    public String getStatus() {
        if (!availability) {
            return "DOWN";
        }
        if (avgLatency > 10000 || failureRate > 0.05) {
            return "DEGRADE";
        }
        return "STABLE";
    }
}
