package com.Api.Fidelitypay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue
    private Long id;

    private String paymentId;
    private String operator;
    private String status; // PENDING, SUCCESS, FAILED
    private double cost;
    private LocalDateTime timestamp;
}