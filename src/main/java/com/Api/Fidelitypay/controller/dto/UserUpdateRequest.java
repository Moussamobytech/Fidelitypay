package com.Api.Fidelitypay.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String fullName;
    private String email;
    private String role;
    private String applicationName;
    private List<String> countries;
    private String callbackUrl;
    private String redirectUrl;
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private Boolean isActive;
}
