package com.Api.Fidelitypay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class LogEntry {

    @Id
    @GeneratedValue
    private Long id;

    private String paymentId;
    private String routeUsed;
    private double responseTime;
    private String status;
    private LocalDateTime timestamp;
}