package com.Api.Fidelitypay.service.routing;

import com.Api.Fidelitypay.model.PaymentProviderRoute;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;

@Component
public class RouteScoreCalculator {

    @Value("${payment.routing.min-sample-size:10}")
    private int minSampleSize;

    @Value("${payment.routing.max-acceptable-failure-rate:0.05}")
    private double maxAcceptableFailureRate;

    public Comparator<PaymentProviderRoute> comparator(Map<Long, Integer> merchantPriorities) {
        Map<Long, Integer> priorities = merchantPriorities == null ? Map.of() : merchantPriorities;
        return Comparator
                .comparingInt(this::reliabilityBucket)
                .thenComparingDouble(this::effectiveFailureRate)
                .thenComparingInt(route -> effectiveMerchantPriority(route,
                        priorities.get(route.getId()),
                        reliabilityBucket(route)))
                .thenComparingDouble(PaymentProviderRoute::getAvgLatency)
                .thenComparingDouble(PaymentProviderRoute::getCost);
    }

    public double score(PaymentProviderRoute route, Integer merchantPriority) {
        int bucket = reliabilityBucket(route);
        double failureRate = effectiveFailureRate(route);
        int priority = effectiveMerchantPriority(route, merchantPriority, bucket);

        return bucket * 1_000_000.0
                + failureRate * 10_000.0
                + priority
                + route.getAvgLatency() * 0.001
                + route.getCost() * 0.5;
    }

    private boolean hasInsufficientSamples(PaymentProviderRoute route) {
        return route.getMetricsSampleCount() < minSampleSize;
    }

    private int reliabilityBucket(PaymentProviderRoute route) {
        if (hasInsufficientSamples(route)) {
            return 0;
        }
        return route.getFailureRate() <= maxAcceptableFailureRate ? 0 : 1;
    }

    private double effectiveFailureRate(PaymentProviderRoute route) {
        return hasInsufficientSamples(route) ? 0.0 : route.getFailureRate();
    }

    private int effectiveMerchantPriority(PaymentProviderRoute route, Integer merchantPriority, int reliabilityBucket) {
        if (reliabilityBucket == 0) {
            return merchantPriority == null ? route.getPriority() : merchantPriority;
        }
        return route.getPriority();
    }
}
