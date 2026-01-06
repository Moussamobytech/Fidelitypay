package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.repository.RouteRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MonitoringService {

    private final RouteRepository routeRepository;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;

    public MonitoringService(
            RouteRepository routeRepository,
            KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient) {
        this.routeRepository = routeRepository;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
    }

    // @Scheduled(fixedRateString = "${monitoring.interval:300000}")
    public void checkRoutes() {

        for (Route route : routeRepository.findAll()) {

            boolean isUp = true; // optimiste
            long start = System.nanoTime();

            try {
                if ("Kkiapay".equalsIgnoreCase(route.getProvider())) {
                    isUp = kkiapayClient.isAvailable();
                } else if ("PAYDUNYA".equalsIgnoreCase(route.getProvider())) {
                    isUp = payDunyaClient.isAvailable();
                } else {
                    log.warn("Unknown provider: {}", route.getProvider());
                }
            } catch (Exception e) {
                log.error("Monitoring error for provider {}", route.getProvider(), e);
                isUp = false;
            }

            double latencyMs = (System.nanoTime() - start) / 1_000_000.0;

            route.setAvailability(isUp);
            route.setAvgLatency(latencyMs);
            routeRepository.save(route);

            log.info("Route {} [{}] -> UP={}, latency={}ms",
                    route.getName(), route.getProvider(), isUp, latencyMs);
        }
    }
}
