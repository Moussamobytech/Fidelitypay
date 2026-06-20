package com.Api.Fidelitypay.controller.dto;

import lombok.Data;

/**
 * DTO for routing fallback configuration.
 * Mirrors the Angular FallbackSettings interface exactly.
 */
@Data
public class FallbackSettingsDto {

    /** Enable automatic failover to next provider after failure. */
    private boolean autoFallback = true;

    /** Enable intelligent retry on 5xx errors. */
    private boolean smartRetry = true;

    /** Number of retry attempts (1-10). */
    private int retryMaxAttempts = 3;

    /** Timeout in seconds before switching to fallback (1-60). */
    private int criticalTimeoutSeconds = 10;
}
