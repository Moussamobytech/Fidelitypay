

package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.CountryOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CountryOptionRepository extends JpaRepository<CountryOption, Long> {

    /**
     * Recherche les options de paiement pour un pays donné.
     * Retourne un Optional pour mieux gérer les cas où le pays n'existe pas.
     */
    Optional<CountryOption> findByCountry(String country);
}
