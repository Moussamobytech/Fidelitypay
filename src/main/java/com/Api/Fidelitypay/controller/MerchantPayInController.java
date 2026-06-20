package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.MerchantApiPrincipal;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentRequest;
import com.Api.Fidelitypay.controller.dto.MerchantPaymentResponse;
import com.Api.Fidelitypay.controller.dto.OtpActionRequest;
import com.Api.Fidelitypay.service.MerchantApiAuthService;
import com.Api.Fidelitypay.service.MerchantPayInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "merchant-pay-in", description = "Public merchant API authenticated with API keys")
public class MerchantPayInController {

    private final MerchantApiAuthService merchantApiAuthService;
    private final MerchantPayInService merchantPayInService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a merchant pay-in", description = "Public API endpoint for merchant servers and SDKs. Uses X-API-Key and Idempotency-Key headers.")
    public ResponseEntity<MerchantPaymentResponse> initiate(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody MerchantPaymentRequest request,
            HttpServletRequest httpRequest) {
        MerchantApiPrincipal principal = authenticate(apiKey, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(merchantPayInService.initiate(principal, request, idempotencyKey));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Read a merchant pay-in")
    public ResponseEntity<MerchantPaymentResponse> getPayment(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable String paymentId,
            HttpServletRequest httpRequest) {
        MerchantApiPrincipal principal = authenticate(apiKey, httpRequest);
        return ResponseEntity.ok(merchantPayInService.getPayment(principal, paymentId));
    }

    @PostMapping("/{paymentId}/actions/otp")
    @Operation(summary = "Submit a required OTP action")
    public ResponseEntity<MerchantPaymentResponse> submitOtp(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable String paymentId,
            @Valid @RequestBody OtpActionRequest request,
            HttpServletRequest httpRequest) {
        MerchantApiPrincipal principal = authenticate(apiKey, httpRequest);
        return ResponseEntity.ok(merchantPayInService.submitOtp(principal, paymentId, request.getOtp()));
    }

    private MerchantApiPrincipal authenticate(String apiKey, HttpServletRequest request) {
        return merchantApiAuthService.authenticate(apiKey, clientIp(request));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
