package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.LogStatus;
import com.Api.Fidelitypay.integration.KkiapayClient;
import com.Api.Fidelitypay.integration.PayDunyaClient;
import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.LogEntryRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MonitoringService {

    private final PaymentProviderRouteRepository routeRepository;
    private final KkiapayClient kkiapayClient;
    private final PayDunyaClient payDunyaClient;
    private final LogEntryRepository logEntryRepository;

    public MonitoringService(PaymentProviderRouteRepository routeRepository, KkiapayClient kkiapayClient,
            PayDunyaClient payDunyaClient, LogEntryRepository logEntryRepository) {
        this.routeRepository = routeRepository;
        this.kkiapayClient = kkiapayClient;
        this.payDunyaClient = payDunyaClient;
        this.logEntryRepository = logEntryRepository;
    }

    public List<LogEntry> getAllLogs() {
        return logEntryRepository.findAll();
    }

    @Scheduled(fixedRateString = "${monitoring.interval:300000}")
    public void checkRoutes() {
        for (PaymentProviderRoute route : routeRepository.findAllWithProviderOrderByCountryAscOperatorAscPriorityAsc()) {
            long start = System.nanoTime();
            boolean isUp;
            String errorMessage = null;

            try {
                isUp = isProviderAvailable(route);
                if (!isUp) {
                    errorMessage = "Service unavailable (health check failed)";
                }
            } catch (Exception e) {
                isUp = false;
                errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
                log.error("Monitoring error for provider route {}", routeName(route), e);
            }

            double latencyMs = (System.nanoTime() - start) / 1_000_000.0;
            route.setLastErrorMessage(isUp ? null : errorMessage);
            List<LogEntry> recentLogs = recentLogs(routeName(route));
            route.setAvgLatency(recentLogs.isEmpty() ? latencyMs : averageLatency(recentLogs));
            route.setFailureRate(failureRate(recentLogs));
            routeRepository.save(route);

            log.info("Provider route {} -> UP={}, latency={}ms, error={}", routeName(route), isUp, latencyMs,
                    errorMessage);
        }
    }

    private boolean isProviderAvailable(PaymentProviderRoute route) {
        String providerCode = route.getProvider().getCode();
        if ("KKIAPAY".equalsIgnoreCase(providerCode)) {
            return kkiapayClient.isAvailable();
        }
        if ("PAYDUNYA".equalsIgnoreCase(providerCode)) {
            return payDunyaClient.isAvailable();
        }
        log.warn("Unknown provider: {}", providerCode);
        return false;
    }

    private List<LogEntry> recentLogs(String routeName) {
        return logEntryRepository.findByRouteUsedAndCreatedAtAfter(routeName, LocalDateTime.now().minusHours(1));
    }

    private double averageLatency(List<LogEntry> recentLogs) {
        return recentLogs.stream()
                .map(LogEntry::getResponseTime)
                .filter(responseTime -> responseTime != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double failureRate(List<LogEntry> recentLogs) {
        if (recentLogs.isEmpty()) {
            return 0.0;
        }
        long failed = recentLogs.stream().filter(log -> log.getStatus() == LogStatus.FAILED).count();
        return (double) failed / recentLogs.size();
    }

    private String routeName(PaymentProviderRoute route) {
        return route.getProvider().getCode() + "_" + route.getOperator() + "_" + route.getCountry();
    }
}
