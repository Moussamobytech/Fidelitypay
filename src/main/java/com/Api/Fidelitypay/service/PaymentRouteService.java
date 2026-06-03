package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import com.Api.Fidelitypay.service.routing.RouteScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        String normalizedEnvironment = normalize(environment == null || environment.isBlank() ? "LIVE" : environment);
        Set<Long> disabledRouteIds = routeSettingService.disabledRouteIdsForMerchant(merchantUserId);
        Map<Long, Integer> merchantPriorities = routeSettingService.routePrioritiesForMerchant(merchantUserId);
        return routeRepository.findAvailable(
                        PaymentDirection.PAYIN,
                        normalize(country),
                        normalize(operator),
                        normalizedEnvironment)
                .stream()
                .filter(route -> !disabledRouteIds.contains(route.getId()))
                .filter(route -> allowGlobalCredentialsFallback || accountService.getEnabledAccount(merchantUserId,
                        route.getProvider().getId(), normalizedEnvironment) != null)
                .sorted(routeScoreCalculator.comparator(merchantPriorities))
                .toList();
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
