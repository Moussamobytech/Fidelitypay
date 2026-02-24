package com.Api.Fidelitypay.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a webhook
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWebhookRequest {

    @NotBlank(message = "URL is required")
    @Size(max = 1000, message = "URL must not exceed 1000 characters")
    @Pattern(regexp = "https?://.*", message = "URL must start with http:// or https://")
    private String url;

    @NotBlank(message = "Event is required")
    @Pattern(regexp = "payment\\.(success|failed|pending|refunded)|.*", message = "Invalid event type")
    private String event;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
