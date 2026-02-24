package com.Api.Fidelitypay.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for API Key response (hiding sensitive data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private String id;
    private String name;
    private String publicKey;

    /**
     * Only shown after creation, null afterwards
     */
    private String secretKey;

    /**
     * Masked version for display (e.g., "sk_live_****1234")
     */
    private String secretKeyMasked;

    private String environment;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private String lastUsedIp;
    private LocalDateTime expiresAt;
    private String metadata;
}
