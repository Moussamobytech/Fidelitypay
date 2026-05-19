package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantApiPrincipal {
    private ApiKey apiKey;
    private User user;
    private String environment;
}
