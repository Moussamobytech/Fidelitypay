package com.Api.Fidelitypay.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApiKeyRequest {

    @NotBlank(message = "Key name is required")
    @Size(min = 3, max = 100, message = "Key name must be between 3 and 100 characters")
    private String name;
}
