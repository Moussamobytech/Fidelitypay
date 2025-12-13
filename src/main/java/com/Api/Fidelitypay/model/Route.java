package com.Api.Fidelitypay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Route {

    @Id
    @GeneratedValue
    private Long id;

    private String name; // ex: "OM via SamirPay"
    private double cost;
    private boolean availability; // UP/DOWN
    private double averageResponseTime;
}