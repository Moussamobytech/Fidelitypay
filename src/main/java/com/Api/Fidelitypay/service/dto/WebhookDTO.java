package com.Api.Fidelitypay.service.dto;

import com.Api.Fidelitypay.enums.ErrorType;
import com.Api.Fidelitypay.enums.FailureStage;
import com.Api.Fidelitypay.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WebhookDTO {
    private String paymentId;
    private String event;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String operator;
    private String country;
    private String providerPaymentId;
    private String failureReason;
    private ErrorType errorType;
    private FailureStage failureStage;
    private LocalDateTime updatedAt;
}
