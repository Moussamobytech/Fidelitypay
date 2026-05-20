package com.Api.Fidelitypay.controller.dto;

import lombok.Data;

import java.util.Map;

@Data
public class MerchantProviderAccountRequest {
    private Long providerId;
    private String environment = "LIVE";
    private Map<String, String> credentials;
    private boolean enabled = true;
}
