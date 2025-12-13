package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/payment-options")
    public ResponseEntity<List<String>> getPaymentOptions(@RequestParam String country) {
        // Retourne les options par pays (ex: OM, Wave)
        List<String> options = paymentService.getOptionsByCountry(country);
        return ResponseEntity.ok(options);
    }

    @PostMapping("/payments/initiate")
    public ResponseEntity<Payment> initiatePayment(@RequestBody Map<String, Object> request) {
        // request contient montant, pays, operator
        double amount = (double) request.get("amount");
        String country = (String) request.get("country");
        String operator = (String) request.get("operator");
        Payment payment = paymentService.initiatePayment(amount, country, operator);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/payments/status/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable String paymentId) {
        Payment payment = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(payment);
    }
}
