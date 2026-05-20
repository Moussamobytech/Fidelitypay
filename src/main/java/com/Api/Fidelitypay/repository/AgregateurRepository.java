package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Agregateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgregateurRepository extends JpaRepository<Agregateur, Long> {
    List<Agregateur> findByOwnerUserId(String ownerUserId);
    List<Agregateur> findByOwnerUserIdAndEnabledFalse(String ownerUserId);
}
