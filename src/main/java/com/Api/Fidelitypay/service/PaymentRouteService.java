package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentRouteService {

    private final PaymentProviderRouteRepository routeRepository;
    private final MerchantPaymentRouteSettingService routeSettingService;
    private final MerchantProviderAccountService accountService;

    @Value("${payment.providers.allow-global-credentials-fallback:false}")
    private boolean allowGlobalCredentialsFallback;

    public List<PaymentProviderRoute> findAvailablePayIn(String country, String operator, String environment,
            String merchantUserId) {
        String normalizedEnvironment = normalize(environment == null || environment.isBlank() ? "LIVE" : environment);
        Set<Long> disabledRouteIds = routeSettingService.disabledRouteIdsForMerchant(merchantUserId);
        Map<Long, Integer> merchantPriorities = routeSettingService.routePrioritiesForMerchant(merchantUserId);
        return routeRepository.findAvailable(
                        PaymentDirection.PAYIN,
                        normalize(country),
                        normalize(operator))
                .stream()
                .filter(route -> supportsEnvironment(route, normalizedEnvironment))
                .filter(route -> !disabledRouteIds.contains(route.getId()))
                .filter(route -> allowGlobalCredentialsFallback || accountService.getEnabledAccount(merchantUserId,
                        route.getProvider().getId(), normalizedEnvironment) != null)
                .sorted(Comparator.comparingDouble(route -> score(route, merchantPriorities.get(route.getId()))))
                .toList();
    }

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

    public static double score(PaymentProviderRoute route, Integer merchantPriority) {
        int effectivePriority = merchantPriority == null ? route.getPriority() : merchantPriority;
        return effectivePriority
                + route.getCost() * 0.5
                + route.getAvgLatency() * 0.3 / 1000.0
                + route.getFailureRate() * 0.2;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
