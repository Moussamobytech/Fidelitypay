package com.Api.Fidelitypay.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for webhook response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    private String id;
    private String url;
    private String event;
    private String description;
    private boolean isActive;
    private LocalDateTime lastTriggeredAt;
    private Integer lastStatusCode;
    private int failureCount;
    private LocalDateTime createdAt;
}
