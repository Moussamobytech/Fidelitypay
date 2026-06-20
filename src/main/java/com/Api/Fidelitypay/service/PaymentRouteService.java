package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.controller.dto.RoutingPreviewResponse;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import com.Api.Fidelitypay.service.routing.RouteScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentRouteService {

    private final PaymentProviderRouteRepository routeRepository;
    private final MerchantPaymentRouteSettingService routeSettingService;
    private final MerchantProviderAccountService accountService;
    private final RouteScoreCalculator routeScoreCalculator;

    @Value("${payment.providers.allow-global-credentials-fallback:false}")
    private boolean allowGlobalCredentialsFallback;

    public List<PaymentProviderRoute> findAvailablePayIn(String country, String operator, String environment,
            String merchantUserId) {
        return evaluatePayIn(country, operator, environment, merchantUserId).routes();
    }

    public RoutingEvaluation evaluatePayIn(String country, String operator, String environment,
            String merchantUserId) {
        String normalizedEnvironment = normalize(environment == null || environment.isBlank() ? "LIVE" : environment);
        Set<Long> disabledRouteIds = routeSettingService.disabledRouteIdsForMerchant(merchantUserId);
        Map<Long, Integer> merchantPriorities = routeSettingService.routePrioritiesForMerchant(merchantUserId);
        List<PaymentProviderRoute> routes = routeRepository.findAvailable(
                        PaymentDirection.PAYIN,
                        normalize(country),
                        normalize(operator))
                .stream()
                .filter(route -> supportsEnvironment(route, normalizedEnvironment))
                .filter(route -> !disabledRouteIds.contains(route.getId()))
                .filter(route -> allowGlobalCredentialsFallback || accountService.getEnabledAccount(merchantUserId,
                        route.getProvider().getId(), normalizedEnvironment) != null)
                .sorted(routeScoreCalculator.comparator(merchantPriorities))
                .toList();
        List<RoutingPreviewResponse.Candidate> candidates = java.util.stream.IntStream.range(0, routes.size())
                .mapToObj(index -> toCandidate(routes.get(index), merchantPriorities.get(routes.get(index).getId()), index + 1))
                .toList();
        return new RoutingEvaluation(routes, RoutingPreviewResponse.builder()
                .country(normalize(country))
                .operator(normalize(operator))
                .environment(normalizedEnvironment)
                .scoringVersion(RouteScoreCalculator.SCORING_VERSION)
                .evaluatedAt(LocalDateTime.now())
                .selected(candidates.isEmpty() ? null : candidates.get(0))
                .candidates(candidates)
                .build());
    }

    private RoutingPreviewResponse.Candidate toCandidate(PaymentProviderRoute route, Integer merchantPriority, int rank) {
        return RoutingPreviewResponse.Candidate.builder()
                .rank(rank)
                .routeId(route.getId())
                .provider(route.getProvider().getCode())
                .flowType(route.getFlowType() == null ? null : route.getFlowType().name())
                .effectivePriority(merchantPriority == null ? route.getPriority() : merchantPriority)
                .cost(route.getCost())
                .avgLatencyMs(route.getAvgLatency())
                .initiationFailureRate(route.getFailureRate())
                .sampleCount(route.getMetricsSampleCount())
                .sufficientSamples(routeScoreCalculator.hasSufficientSamples(route))
                .score(routeScoreCalculator.score(route, merchantPriority))
                .build();
    }

    public record RoutingEvaluation(List<PaymentProviderRoute> routes, RoutingPreviewResponse preview) {}

    private boolean supportsEnvironment(PaymentProviderRoute route, String environment) {
        return "SANDBOX".equals(environment) ? route.isSandboxEnabled() : route.isLiveEnabled();
    }

    public List<String> findAvailablePayInOperators(String country) {
        return routeRepository.findAvailableOperators(PaymentDirection.PAYIN, normalize(country));
    }

    public boolean hasAnyPayInRoute(String country, String operator) {
        return !routeRepository.findByDirectionAndCountryAndOperatorOrderByPriorityAsc(
                PaymentDirection.PAYIN,
                normalize(country),
                normalize(operator)).isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
