package com.Api.Fidelitypay.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantPaymentRequest {

    @Min(value = 1, message = "amount must be an integer XOF amount greater than 0")
    private long amount;

    @NotBlank
    @Pattern(regexp = "XOF", flags = Pattern.Flag.CASE_INSENSITIVE, message = "Only XOF is supported for pay-in")
    private String currency = "XOF";

    @NotBlank
    @Size(min = 2, max = 3)
    private String country;

    @NotBlank
    @Size(max = 50)
    private String operator;

    @Valid
    @NotNull
    private Customer customer;

    @Size(max = 1000)
    private String returnUrl;

    @Size(max = 1000)
    private String cancelUrl;

    @Data
    public static class Customer {
        @Size(max = 50)
        private String phone;

        @NotBlank
        @Size(max = 120)
        private String firstname;

        @NotBlank
        @Size(max = 120)
        private String lastname;

        @Size(max = 255)
        private String email;
    }
}
