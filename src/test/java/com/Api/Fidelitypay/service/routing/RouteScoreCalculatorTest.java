package com.Api.Fidelitypay.service.routing;

import com.Api.Fidelitypay.model.PaymentProviderRoute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteScoreCalculatorTest {

    private RouteScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RouteScoreCalculator();
        ReflectionTestUtils.setField(calculator, "minSampleSize", 10);
        ReflectionTestUtils.setField(calculator, "maxAcceptableFailureRate", 0.05);
    }

    @Test
    void prefersReliableOverCheapUnreliable() {
        PaymentProviderRoute reliable = route(1L, 20, 0.01, 100, 500.0, 10.0);
        PaymentProviderRoute unreliable = route(2L, 20, 0.10, 100, 500.0, 1.0);

        List<PaymentProviderRoute> sorted = sort(reliable, unreliable);

        assertEquals(reliable.getId(), sorted.get(0).getId());
        assertTrue(calculator.score(reliable, null) < calculator.score(unreliable, null));
    }

    @Test
    void doesNotPenalizeInsufficientSamples() {
        PaymentProviderRoute fewSamplesHighFailure = route(1L, 3, 0.333, 100, 500.0, 10.0);
        PaymentProviderRoute enoughSamplesUnreliable = route(2L, 20, 0.10, 100, 500.0, 1.0);

        List<PaymentProviderRoute> sorted = sort(fewSamplesHighFailure, enoughSamplesUnreliable);

        assertEquals(fewSamplesHighFailure.getId(), sorted.get(0).getId());
        assertEquals(0, (int) ReflectionTestUtils.invokeMethod(calculator, "reliabilityBucket", fewSamplesHighFailure));
    }

    @Test
    void merchantPriorityWhenBothReliable() {
        PaymentProviderRoute preferred = route(1L, 20, 0.01, 100, 500.0, 10.0);
        PaymentProviderRoute fallback = route(2L, 20, 0.01, 100, 500.0, 10.0);
        Map<Long, Integer> merchantPriorities = Map.of(
                preferred.getId(), 10,
                fallback.getId(), 50);

        List<PaymentProviderRoute> sorted = sort(merchantPriorities, preferred, fallback);

        assertEquals(preferred.getId(), sorted.get(0).getId());
    }

    @Test
    void ignoresMerchantPriorityWhenUnreliable() {
        PaymentProviderRoute reliable = route(1L, 20, 0.01, 100, 500.0, 10.0);
        PaymentProviderRoute unreliable = route(2L, 20, 0.10, 100, 500.0, 1.0);
        Map<Long, Integer> merchantPriorities = Map.of(
                reliable.getId(), 100,
                unreliable.getId(), 1);

        List<PaymentProviderRoute> sorted = sort(merchantPriorities, reliable, unreliable);

        assertEquals(reliable.getId(), sorted.get(0).getId());
    }

    @Test
    void latencyBeforeCost() {
        PaymentProviderRoute faster = route(1L, 20, 0.01, 100, 100.0, 5.0);
        PaymentProviderRoute cheaper = route(2L, 20, 0.01, 100, 200.0, 1.0);

        List<PaymentProviderRoute> sorted = sort(faster, cheaper);

        assertEquals(faster.getId(), sorted.get(0).getId());
    }

    private List<PaymentProviderRoute> sort(PaymentProviderRoute... routes) {
        return sort(Map.of(), routes);
    }

    private List<PaymentProviderRoute> sort(Map<Long, Integer> merchantPriorities, PaymentProviderRoute... routes) {
        Comparator<PaymentProviderRoute> comparator = calculator.comparator(merchantPriorities);
        return java.util.Arrays.stream(routes).sorted(comparator).toList();
    }

    private PaymentProviderRoute route(long id, int sampleCount, double failureRate, int priority,
            double avgLatency, double cost) {
        PaymentProviderRoute route = new PaymentProviderRoute();
        route.setId(id);
        route.setMetricsSampleCount(sampleCount);
        route.setFailureRate(failureRate);
        route.setPriority(priority);
        route.setAvgLatency(avgLatency);
        route.setCost(cost);
        return route;
    }
}
