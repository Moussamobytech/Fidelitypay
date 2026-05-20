package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import lombok.Data;

@Data
public class PaymentProviderRequest {
    private String code;
    private String displayName;
    private PaymentProviderStatus status = PaymentProviderStatus.ACTIVE;
    private String credentialSchema;
}
