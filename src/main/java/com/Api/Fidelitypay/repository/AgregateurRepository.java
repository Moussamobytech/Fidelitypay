package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Agregateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgregateurRepository extends JpaRepository<Agregateur, Long> {
<<<<<<< HEAD
    List<Agregateur> findByOwnerUserId(String ownerUserId);
    List<Agregateur> findByOwnerUserIdAndEnabledFalse(String ownerUserId);
=======
    java.util.Optional<Agregateur> findByNomAIgnoreCase(String nomA);
>>>>>>> 6451fc7ea20468a53eca0812ef46cd8840cb6a75
}
