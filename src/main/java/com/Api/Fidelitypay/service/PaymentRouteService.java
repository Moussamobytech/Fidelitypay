package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.PaymentRoute;
import com.Api.Fidelitypay.repository.PaymentRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentRouteService {

    private final PaymentRouteRepository repository;
    private final AgregateurService agregateurService;

    public List<PaymentRoute> findAvailablePayIn(String country, String operator, String environment) {
        return repository.findByDirectionAndEnabledTrueAndObservedUpTrueAndCountryAndOperatorAndEnvironmentOrderByPriorityAsc(
                PaymentDirection.PAYIN,
                normalize(country),
                normalize(operator),
                normalize(environment == null || environment.isBlank() ? "LIVE" : environment));
    }

    public List<PaymentRoute> findAvailablePayIn(String country, String operator, String environment, String merchantUserId) {
        List<PaymentRoute> routes = findAvailablePayIn(country, operator, environment);
        Set<String> disabledProviders = agregateurService.getDisabledProviderNamesForMerchant(merchantUserId);
        if (disabledProviders.isEmpty()) {
            return routes;
        }
        return routes.stream()
                .filter(route -> !disabledProviders.contains(normalize(route.getProvider())))
                .toList();
    }

    public boolean hasAnyPayInRoute(String country, String operator) {
        return !repository.findByDirectionAndCountryAndOperatorOrderByPriorityAsc(
                PaymentDirection.PAYIN,
                normalize(country),
                normalize(operator)).isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
