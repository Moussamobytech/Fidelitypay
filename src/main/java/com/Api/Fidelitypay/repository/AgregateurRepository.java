package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Agregateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgregateurRepository extends JpaRepository<Agregateur, Long> {
    java.util.Optional<Agregateur> findByNomAIgnoreCase(String nomA);
}
