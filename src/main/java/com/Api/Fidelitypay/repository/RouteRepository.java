package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /**
     * Routes disponibles (UP) pour un opérateur donné.
     */
    List<Route> findByAvailabilityTrueAndOperator(String operator);

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
}
