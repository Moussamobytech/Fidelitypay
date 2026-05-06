package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * Toutes les routes disponibles (UP).
     */
    List<Route> findByAvailabilityTrue();

    /**
     * Routes disponibles (UP) pour un opérateur et un pays donnés.
     */
    List<Route> findByAvailabilityTrueAndOperatorAndCountry(String operator, String country);

    /**
     * Toutes les routes disponibles (UP) pour un opérateur donné.
     */
    List<Route> findByAvailabilityTrueAndOperator(String operator);

    /**
     * Routes disponibles (UP) pour un opérateur sans pays spécifié (global).
     */
    List<Route> findByAvailabilityTrueAndOperatorAndCountryIsNull(String operator);

    /**
     * Routes disponibles (UP) pour un opérateur et un provider donné.
     */
    List<Route> findByAvailabilityTrueAndOperatorAndProvider(String operator, String provider);

    /**
     * Toutes les routes d’un opérateur, même indisponibles.
     */
    List<Route> findByOperator(String operator);

    /**
     * Routes d’un provider spécifique.
     */
    List<Route> findByProvider(String provider);

    /**
     * Routes avec coût inférieur à une valeur donnée.
     */
    List<Route> findByOperatorAndCostLessThan(String operator, double maxCost);

    /**
     * Routes triées par latence croissante.
     */
    List<Route> findByOperatorOrderByAvgLatencyAsc(String operator);

    /**
     * Recherche d’une route par son nom exact.
     */
    Optional<Route> findByName(String name);

    /**
     * Vérifie si une route existe par son nom.
     */
    boolean existsByName(String name);

    /**
     * Récupère les opérateurs distincts disponibles pour un pays spécifique (ou globaux).
     */
    @Query("SELECT DISTINCT r.operator FROM Route r WHERE r.availability = true AND (LOWER(r.country) = LOWER(:country) OR r.country IS NULL OR r.country = '') AND r.operator IS NOT NULL")
    List<String> findDistinctOperatorsByCountry(@Param("country") String country);
}
