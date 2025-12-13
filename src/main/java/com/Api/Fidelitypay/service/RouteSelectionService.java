package com.Api.Fidelitypay.service;



import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteSelectionService {

    @Autowired
    private RouteRepository routeRepository;

    public Route selectBestRoute(String operator) {
        // Récupérer routes UP pour l'opérateur (filtre par name contenant operator)
        List<Route> availableRoutes = routeRepository.findByAvailabilityTrue()
                .stream()
                .filter(r -> r.getName().contains(operator))
                .sorted(Comparator.comparingDouble(Route::getCost))
                .collect(Collectors.toList());

        return availableRoutes.isEmpty() ? null : availableRoutes.get(0);
    }

    public Route selectFallbackRoute(String operator) {
        // Similaire, mais skip la première
        List<Route> availableRoutes = routeRepository.findByAvailabilityTrue()
                .stream()
                .filter(r -> r.getName().contains(operator))
                .sorted(Comparator.comparingDouble(Route::getCost))
                .collect(Collectors.toList());

        return availableRoutes.size() < 2 ? null : availableRoutes.get(1);
    }
}
