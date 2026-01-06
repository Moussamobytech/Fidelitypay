// package com.Api.Fidelitypay.controller;

// import com.Api.Fidelitypay.model.Payment;
// import com.Api.Fidelitypay.service.PaymentService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.*;

// import java.util.List;

// @RestController
// @RequestMapping("/api")
// public class PaymentController {

//     @Autowired
//     private PaymentService paymentService;

//     @GetMapping("/payment-options")
//     public ResponseEntity<List<String>> getPaymentOptions(@RequestParam String country) {
//         // Retourne les options par pays (ex: OM, Wave)
//         List<String> options = paymentService.getOptionsByCountry(country);
//         return ResponseEntity.ok(options);
//     }

//     @PostMapping("/payments/initiate")
//     public ResponseEntity<Payment> initiatePayment(@jakarta.validation.Valid @RequestBody com.Api.Fidelitypay.controller.dto.PaymentInitiateRequest request) {
//         double amount = request.getAmount();
//         String country = request.getCountry();
//         String operator = request.getOperator();
//         Payment payment = paymentService.initiatePayment(amount, country, operator);
//         return ResponseEntity.ok(payment);
//     }

//     @GetMapping("/payments/status/{paymentId}")
//     public ResponseEntity<Payment> getPaymentStatus(@PathVariable String paymentId) {
//         Payment payment = paymentService.getPaymentStatus(paymentId);
//         return ResponseEntity.ok(payment);
//     }
// }

package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.PaymentInitiateRequest;
import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Liste des moyens de paiement disponibles par pays
     */
    @GetMapping("/payment-options")
    public ResponseEntity<List<String>> getPaymentOptions(
            @RequestParam String country) {

        return ResponseEntity.ok(
                paymentService.getOptionsByCountry(country));
    }

    /**
     * Initialisation d’un paiement
     */
    @PostMapping("/payments/initiate")
    public ResponseEntity<Payment> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request) {

        Payment payment = paymentService.initiatePayment(
                request.getAmount(),
                request.getCountry(),
                request.getOperator(),
                request.getPhone());

        return ResponseEntity.ok(payment);
    }

    /**
     * Statut d’un paiement
     */
    @GetMapping("/payments/status/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(
            @PathVariable String paymentId) {

        Payment payment = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(payment);
    }
}
