package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitoringService {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private KkiapayClient samirPayClient;

    @Autowired
    private PayDunyaClient payDunyaClient;

    @Scheduled(fixedRate = 300000) // toutes les 5 minutes
    public void checkRoutes() {
        List<Route> routes = routeRepository.findAll();
        for (Route route : routes) {
            boolean isUp = false;
            double latencyMs = 0.0;
            long start = System.nanoTime();

            // Vérification de la disponibilité selon le provider
            if ("Kkiapay".equalsIgnoreCase(route.getProvider())) {
                isUp = samirPayClient.isAvailable();
            } else if ("PAYDUNYA".equalsIgnoreCase(route.getProvider())) {
                isUp = payDunyaClient.isAvailable();
            }

            long end = System.nanoTime();
            latencyMs = (end - start) / 1_000_000.0;

            // Mise à jour de la route
            route.setAvailability(isUp);
            route.setAvgLatency(latencyMs);
            routeRepository.save(route);
        }
    }
}