package com.Api.Fidelitypay.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class MerchantProviderAccountResponse {
    private Long id;
    private Long providerId;
    private String providerCode;
    private String providerDisplayName;
    private String environment;
    private boolean enabled;
    private Map<String, String> credentialHints;
}
