package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.Api.Fidelitypay.Enum.PaymentStatus;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_payment_id", columnList = "paymentId"),
        @Index(name = "idx_payment_status", columnList = "status")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId;

    @Column(nullable = false)
    private String operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private double amount; // Montant payé par le client

    @Column(nullable = false)
    private double cost; // Coût de la route (agrégateur)

    @Column(nullable = false, length = 3)
    private String currency; // XOF, EUR, USD

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
