package com.Api.Fidelitypay.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new API key
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    @NotBlank(message = "Key name is required")
    @Size(min = 3, max = 100, message = "Key name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Environment is required")
    @Pattern(regexp = "sandbox|live", message = "Environment must be 'sandbox' or 'live'")
    private String environment;

}
