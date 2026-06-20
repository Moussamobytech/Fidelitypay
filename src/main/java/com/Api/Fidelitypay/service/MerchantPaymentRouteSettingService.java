package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteResponse;
import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.MerchantPaymentRouteSetting;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.MerchantPaymentRouteSettingRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import com.Api.Fidelitypay.service.routing.RouteScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantPaymentRouteSettingService {

    private final PaymentProviderRouteRepository routeRepository;
    private final MerchantPaymentRouteSettingRepository settingRepository;
    private final PaymentProviderService paymentProviderService;
    private final RouteScoreCalculator routeScoreCalculator;

    @Transactional(readOnly = true)
    public List<PaymentProviderRouteResponse> listMerchantPayInRoutes(String userId) {
        Map<Long, MerchantPaymentRouteSetting> settingsByRoute = settingsByRoute(userId);
        return routeRepository.findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(PaymentDirection.PAYIN)
                .stream()
                .filter(PaymentProviderRoute::isEnabled)
                .map(route -> {
                    MerchantPaymentRouteSetting setting = settingsByRoute.get(route.getId());
                    Integer merchantPriority = setting == null ? null : setting.getPriority();
                    return paymentProviderService.toRouteResponse(route,
                            setting == null ? null : setting.isEnabled(),
                            merchantPriority,
                            routeScoreCalculator.score(route, merchantPriority));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProviderRouteResponse> listPlatformPayInRoutes() {
        return routeRepository.findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(PaymentDirection.PAYIN)
                .stream()
                .map(route -> paymentProviderService.toRouteResponse(route, null, null,
                        routeScoreCalculator.score(route, null)))
                .toList();
    }

    @Transactional
    public PaymentProviderRouteResponse setMerchantRouteEnabled(String userId, Long routeId, boolean enabled) {
        PaymentProviderRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider route not found"));
        MerchantPaymentRouteSetting setting = settingRepository.findByUserIdAndPaymentProviderRouteId(userId, routeId)
                .orElseGet(() -> {
                    MerchantPaymentRouteSetting created = new MerchantPaymentRouteSetting();
                    created.setUserId(userId);
                    created.setPaymentProviderRoute(route);
                    return created;
                });
        setting.setEnabled(enabled);
        MerchantPaymentRouteSetting saved = settingRepository.save(setting);
        return paymentProviderService.toRouteResponse(route, saved.isEnabled(), saved.getPriority(),
                routeScoreCalculator.score(route, saved.getPriority()));
    }

    @Transactional
    public PaymentProviderRouteResponse setPlatformRouteEnabled(Long routeId, boolean enabled) {
        PaymentProviderRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider route not found"));
        route.setEnabled(enabled);
        PaymentProviderRoute saved = routeRepository.save(route);
        return paymentProviderService.toRouteResponse(saved, null, null, routeScoreCalculator.score(saved, null));
    }

    @Transactional
    public PaymentProviderRouteResponse setMerchantRoutePriority(String userId, Long routeId, Integer priority) {
        PaymentProviderRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider route not found"));
        MerchantPaymentRouteSetting setting = settingRepository.findByUserIdAndPaymentProviderRouteId(userId, routeId)
                .orElseGet(() -> {
                    MerchantPaymentRouteSetting created = new MerchantPaymentRouteSetting();
                    created.setUserId(userId);
                    created.setPaymentProviderRoute(route);
                    return created;
                });
        setting.setPriority(priority);
        MerchantPaymentRouteSetting saved = settingRepository.save(setting);
        return paymentProviderService.toRouteResponse(route, saved.isEnabled(), saved.getPriority(),
                routeScoreCalculator.score(route, saved.getPriority()));
    }

    @Transactional(readOnly = true)
    public Set<Long> disabledRouteIdsForMerchant(String userId) {
        return settingsByRoute(userId).values().stream()
                .filter(setting -> !setting.isEnabled())
                .map(setting -> setting.getPaymentProviderRoute().getId())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> routePrioritiesForMerchant(String userId) {
        return settingsByRoute(userId).values().stream()
                .filter(setting -> setting.getPriority() != null)
                .collect(Collectors.toMap(setting -> setting.getPaymentProviderRoute().getId(),
                        MerchantPaymentRouteSetting::getPriority));
    }

    private Map<Long, MerchantPaymentRouteSetting> settingsByRoute(String userId) {
        if (userId == null || userId.isBlank()) {
            return Map.of();
        }
        return settingRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(setting -> setting.getPaymentProviderRoute().getId(), Function.identity()));
    }
}
