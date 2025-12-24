package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class RouteSelectionService {

    private final RouteRepository routeRepository;

    public RouteSelectionService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    /**
     * Sélection de la meilleure route disponible pour un opérateur
     */
    public Route selectBestRoute(String operator) {
        List<Route> routes = getSortedRoutes(operator);
        if (routes.isEmpty()) {
            log.warn("No available route for operator {}", operator);
            return null;
        }

        Route selected = routes.get(0);
        log.info("Best route selected: {}", selected.getName());
        return selected;
    }

    /**
     * Sélection de la route fallback si la principale échoue
     */
    public Route selectFallbackRoute(String operator) {
        List<Route> routes = getSortedRoutes(operator);
        if (routes.size() < 2) {
            log.warn("No fallback route for operator {}", operator);
            return null;
        }

        Route fallback = routes.get(1);
        log.info("Fallback route selected: {}", fallback.getName());
        return fallback;
    }

    /**
     * Récupère les routes disponibles et les trie par score
     */
    private List<Route> getSortedRoutes(String operator) {
        return routeRepository.findByAvailabilityTrueAndOperator(operator)
                .stream()
                .sorted(Comparator.comparingDouble(this::calculateScore))
                .toList();
    }

    /**
     * Calcul du score global de la route
     * Plus le score est bas, meilleure est la route
     */
    private double calculateScore(Route route) {
        double costWeight = 0.5;
        double latencyWeight = 0.3;
        double failureWeight = 0.2;

        return (route.getCost() * costWeight)
                + (route.getAvgLatency() * latencyWeight / 1000)
                + (route.getFailureRate() * failureWeight)
                + route.getPriority();
    }
}
