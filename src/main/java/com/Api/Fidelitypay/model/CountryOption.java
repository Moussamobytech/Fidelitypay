package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "country_options")
public class CountryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2)
    private String country; // "ML", "SN", etc.

    @ElementCollection
    @CollectionTable(name = "country_operator_options", joinColumns = @JoinColumn(name = "country_option_id"))
    @Column(name = "operator")
    private List<String> operators;
}
