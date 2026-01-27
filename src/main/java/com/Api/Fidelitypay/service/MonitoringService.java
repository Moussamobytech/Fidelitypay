package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.Enum.ErrorType;
import com.Api.Fidelitypay.repository.RouteRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MonitoringService {

    private final RouteRepository routeRepository;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;
    private final com.Api.Fidelitypay.repository.LogEntryRepository logEntryRepository;

    public MonitoringService(
            RouteRepository routeRepository,
            KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient,
            com.Api.Fidelitypay.repository.LogEntryRepository logEntryRepository) {
        this.routeRepository = routeRepository;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
        this.logEntryRepository = logEntryRepository;
    }

    public java.util.List<com.Api.Fidelitypay.model.LogEntry> getAllLogs() {
        return logEntryRepository.findAll();
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRateString = "${monitoring.interval:300000}")
    public void checkRoutes() {

        for (Route route : routeRepository.findAll()) {

            boolean isUp = true; // optimiste
            long start = System.nanoTime();

            String errorMessage = null;
            ErrorType errorType = null;

            try {
                if ("Kkiapay".equalsIgnoreCase(route.getProvider())) {
                    isUp = kkiapayClient.isAvailable();
                } else if ("PAYDUNYA".equalsIgnoreCase(route.getProvider())) {
                    isUp = payDunyaClient.isAvailable();
                } else {
                    log.warn("Unknown provider: {}", route.getProvider());
                    errorMessage = "Unknown provider: " + route.getProvider();
                    errorType = ErrorType.UNKNOWN;
                    isUp = false;
                }

                if (!isUp && errorMessage == null) {
                    errorMessage = "Service unavailable (Health check failed)";
                    errorType = ErrorType.PROVIDER_DOWN;
                }
            } catch (Exception e) {
                log.error("Monitoring error for provider {}", route.getProvider(), e);
                isUp = false;

                if (e instanceof java.net.SocketTimeoutException
                        || e.getCause() instanceof java.net.SocketTimeoutException) {
                    errorType = ErrorType.TIMEOUT;
                    errorMessage = "Timeout: le provider ne répond pas";
                } else if (e instanceof java.net.UnknownHostException
                        || e.getCause() instanceof java.net.UnknownHostException) {
                    errorType = ErrorType.NETWORK;
                    errorMessage = "Problème réseau ou DNS";
                } else if (e.getMessage() != null && e.getMessage().contains("401")) {
                    errorType = ErrorType.AUTHENTICATION;
                    errorMessage = "Clé API invalide";
                } else if (e.getMessage() != null && e.getMessage().contains("400")) {
                    errorType = ErrorType.BAD_REQUEST;
                    errorMessage = "Mauvaise requête (400)";
                } else if (e.getMessage() != null
                        && (e.getMessage().contains("500") || e.getMessage().contains("503"))) {
                    errorType = ErrorType.PROVIDER_DOWN;
                    errorMessage = "Serveur du provider HS";
                } else {
                    errorType = ErrorType.UNKNOWN;
                    errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                }
            }

            if (isUp) {
                errorMessage = null;
                errorType = null;
            }

            double latencyMs = (System.nanoTime() - start) / 1_000_000.0;

            // Calculate Failure Rate based on logs from the last 1 hour
            java.time.LocalDateTime oneHourAgo = java.time.LocalDateTime.now().minusHours(1);
            java.util.List<com.Api.Fidelitypay.model.LogEntry> recentLogs = logEntryRepository
                    .findByRouteUsedAndCreatedAtAfter(route.getName(), oneHourAgo);

            if (!recentLogs.isEmpty()) {
                long total = recentLogs.size();
                long failed = recentLogs.stream()
                        .filter(l -> l.getStatus() == com.Api.Fidelitypay.Enum.LogStatus.FAILED).count();
                double failureRate = (double) failed / total;
                route.setFailureRate(failureRate);
            } else {
                route.setFailureRate(0.0);
            }

            route.setAvailability(isUp);
            route.setLastErrorMessage(errorMessage);
            route.setLastErrorType(errorType);
            route.setAvgLatency(latencyMs);
            routeRepository.save(route);

            log.info("Route {} [{}] -> UP={}, latency={}ms, error={}, type={}",
                    route.getName(), route.getProvider(), isUp, latencyMs, errorMessage, errorType);
        }
    }

    public java.util.List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }
}
