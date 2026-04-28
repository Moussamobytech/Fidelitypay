package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            log.warn("No available route for operator {} in country {}", operator, country);
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
     * Récupère les opérateurs distincts disponibles pour un pays donné
     */
    public List<String> getAvailableOperatorsByCountry(String country) {
        return routeRepository.findDistinctOperatorsByCountry(country);
    }

    /**
     * Récupère TOUTES les routes disponibles (UP) en ignorant la contrainte stricte d'opérateur,
     * puis les trie par pertinence pour le pays ciblé,
     * et retourne une seule route par provider (pour éviter d'essayer 2 fois le même agrégateur).
     */
    public List<Route> getSortedRoutes(String operator, String targetCountry) {
        // 1. Récupérer TOUTES les routes disponibles sans se soucier de l'opérateur
        List<Route> allRoutes = routeRepository.findByAvailabilityTrue();

        // 2. Trier ces routes par notre système de score (qui valorise le pays et l'opérateur s'il correspond)
        List<Route> sortedRoutes = allRoutes.stream()
                .filter(r -> r.getCountry() == null || r.getCountry().isEmpty() || r.getCountry().equalsIgnoreCase(targetCountry))
                .sorted(Comparator.comparingDouble(r -> calculateScore(r, targetCountry, operator)))
                .toList();

        // 3. Ne garder qu'une seule route par Provider pour construire le pipeline de Fallback
        // Par exemple: 1er KKIAPAY, 2ème PAYDUNYA
        Map<String, Route> uniqueProviders = new LinkedHashMap<>();
        for (Route route : sortedRoutes) {
            uniqueProviders.putIfAbsent(route.getProvider().toUpperCase(), route);
        }

        return new ArrayList<>(uniqueProviders.values());
    }

    /**
     * Calcul du score global de la route avec affinité géographique et opérateur
     */
    private double calculateScore(Route route, String targetCountry, String targetOperator) {
        double costWeight = 0.5;
        double latencyWeight = 0.3;
        double failureWeight = 0.2;

        double baseScore = (route.getCost() * costWeight)
                + (route.getAvgLatency() * latencyWeight / 1000)
                + (route.getFailureRate() * failureWeight)
                + route.getPriority();

        // Bonus : Si la route a été spécifiquement créée pour cet opérateur, on la privilégie.
        if (targetOperator != null && route.getOperator() != null && route.getOperator().equalsIgnoreCase(targetOperator)) {
            baseScore -= 5.0; 
        }

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
