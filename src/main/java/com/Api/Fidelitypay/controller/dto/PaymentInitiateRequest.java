package com.Api.Fidelitypay.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Objet d’échange
 */
public class PaymentInitiateRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private Double amount;

    @NotBlank
    private String country;

    @NotBlank
    private String operator;

    public PaymentInitiateRequest() {
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
