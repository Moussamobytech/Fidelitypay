package com.Api.Fidelitypay.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private String fullName;
    private String role;
    private String applicationName;
    private List<String> countries;
    private LocalDateTime createdAt;
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private boolean isActive;
}
