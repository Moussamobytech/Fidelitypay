package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.CountryOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryOptionRepository extends JpaRepository<CountryOption, Long> {
    CountryOption findByCountry(String country);
}