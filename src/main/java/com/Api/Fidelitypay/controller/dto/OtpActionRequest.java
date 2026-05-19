package com.Api.Fidelitypay.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpActionRequest {
    @NotBlank
    @Size(min = 3, max = 12)
    private String otp;
}
