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
     * Sélection de la meilleure route disponible pour un opérateur et un pays
     */
    public Route selectBestRoute(String operator, String country) {
        List<Route> routes = getSortedRoutes(operator, country);
        if (routes.isEmpty()) {
            log.warn("No available route for operator {}", operator);
            return null;
        }

        Route selected = routes.get(0);
        log.info("Best route selected for operator {}: {} (Preferred country: {})", operator, selected.getName(),
                country);
        return selected;
    }

    /**
     * Sélection de la route fallback si la principale échoue
     */
    public Route selectFallbackRoute(String operator, String country) {
        List<Route> routes = getSortedRoutes(operator, country);
        if (routes.size() < 2) {
            log.warn("No fallback route for operator {} in country {}", operator, country);
            return null;
        }

        Route fallback = routes.get(1);
        log.info("Fallback route selected for operator {}: {}", operator, fallback.getName());
        return fallback;
    }

    /**
     * Récupère TOUTES les routes disponibles de l'opérateur et les trie
     * intelligemment
     * en favorisant le pays cible.
     */
    public List<Route> getSortedRoutes(String operator, String targetCountry) {
        List<Route> allRoutes = routeRepository.findByAvailabilityTrueAndOperator(operator);

        return allRoutes.stream()
                .sorted(Comparator.comparingDouble(r -> calculateScore(r, targetCountry)))
                .toList();
    }

    /**
     * Calcul du score global de la route avec affinité géographique
     */
    private double calculateScore(Route route, String targetCountry) {
        double costWeight = 0.5;
        double latencyWeight = 0.3;
        double failureWeight = 0.2;

        double baseScore = (route.getCost() * costWeight)
                + (route.getAvgLatency() * latencyWeight / 1000)
                + (route.getFailureRate() * failureWeight)
                + route.getPriority();

        // LOGIQUE D'AFFINITÉ GÉOGRAPHIQUE
        // Priorité 1 : Le pays correspond parfaitement
        if (targetCountry != null && route.getCountry() != null && route.getCountry().equalsIgnoreCase(targetCountry)) {
            return baseScore;
        }

        // Priorité 2 : Route globale (pas de pays défini)
        if (route.getCountry() == null || route.getCountry().isEmpty()) {
            return baseScore + 10.0;
        }

        // Priorité 3 : Autre pays (Dernier recours)
        return baseScore + 1000.0;
    }
}
