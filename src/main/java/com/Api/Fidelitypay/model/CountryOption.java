package com.Api.Fidelitypay.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class CountryOption {

    @Id
    @GeneratedValue
    private Long id;

    private String country;
    private String options; // JSON ou string séparée par virgules des opérateurs
}