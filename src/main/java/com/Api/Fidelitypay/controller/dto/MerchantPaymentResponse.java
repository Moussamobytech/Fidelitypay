package com.Api.Fidelitypay.controller.dto;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.FailureStage;
import com.Api.Fidelitypay.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MerchantPaymentResponse {
    private String paymentId;
    private PaymentStatus status;
    private String paymentUrl;
    private String provider;
    private String operator;
    private String country;
    private BigDecimal amount;
    private String currency;
    private NextAction nextAction;
    private String failureReason;
    private ErrorType errorType;
    private FailureStage failureStage;

    @Data
    @Builder
    public static class NextAction {
        private String type;
        private String provider;
        private String message;
    }
}
