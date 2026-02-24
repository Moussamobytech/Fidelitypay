package com.Api.Fidelitypay.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for API activity/logs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiActivityResponse {

    private Long id;
    private String method;
    private String endpoint;
    private int statusCode;
    private String status;
    private String ipAddress;
    private long latencyMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
