package com.Api.Fidelitypay.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    /** Full API key, returned only when the key is created. */
    private String apiKey;

    /**
     * Only shown after creation, null afterwards
     */
    /** Masked key shown in subsequent list responses. */
    private String apiKeyMasked;

    @JsonProperty("isActive")
    private boolean isActive;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUsedAt;
    private String lastUsedIp;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;
    private String userFullName;
    private String userEmail;
}
