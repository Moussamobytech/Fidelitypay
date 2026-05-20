package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentProviderResponse {
    private Long id;
    private String code;
    private String displayName;
    private PaymentProviderStatus status;
    private String credentialSchema;
}
