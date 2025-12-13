package com.Api.Fidelitypay.service;


// package com.fidelitypay.service;

import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.integration.SamirPayClient;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;  // Added this import to fix the error

@Service
public class MonitoringService {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private SamirPayClient samirPayClient;

    @Autowired
    private PayDunyaClient payDunyaClient;

    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    public void checkRoutes() {
        // Pour chaque route, ping l'API externe
        List<Route> routes = routeRepository.findAll();
        for (Route route : routes) {
            boolean isUp = false;
            if (route.getName().contains("SamirPay")) {
                isUp = samirPayClient.isAvailable();
            } else if (route.getName().contains("PayDunya")) {
                isUp = payDunyaClient.isAvailable();
            }
            // Mettre à jour availability et averageResponseTime (mesurer temps)
            route.setAvailability(isUp);
            routeRepository.save(route);
        }
    }
}