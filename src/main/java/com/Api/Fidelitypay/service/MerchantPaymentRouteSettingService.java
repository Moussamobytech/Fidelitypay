package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteResponse;
import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.MerchantPaymentRouteSetting;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.MerchantPaymentRouteSettingRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
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

    @Transactional(readOnly = true)
    public List<PaymentProviderRouteResponse> listMerchantPayInRoutes(String userId) {
        Map<Long, MerchantPaymentRouteSetting> settingsByRoute = settingRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(setting -> setting.getPaymentProviderRoute().getId(), Function.identity()));
        return routeRepository.findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(PaymentDirection.PAYIN)
                .stream()
                .map(route -> paymentProviderService.toRouteResponse(route,
                        settingsByRoute.containsKey(route.getId())
                                ? settingsByRoute.get(route.getId()).isEnabled()
                                : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProviderRouteResponse> listPlatformPayInRoutes() {
        return routeRepository.findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(PaymentDirection.PAYIN)
                .stream()
                .map(route -> paymentProviderService.toRouteResponse(route, null))
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
        return paymentProviderService.toRouteResponse(route, settingRepository.save(setting).isEnabled());
    }

    @Transactional
    public PaymentProviderRouteResponse setPlatformRouteEnabled(Long routeId, boolean enabled) {
        PaymentProviderRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider route not found"));
        route.setEnabled(enabled);
        return paymentProviderService.toRouteResponse(routeRepository.save(route), null);
    }

    @Transactional(readOnly = true)
    public Set<Long> disabledRouteIdsForMerchant(String userId) {
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }
        return settingRepository.findByUserIdAndEnabledFalse(userId).stream()
                .map(setting -> setting.getPaymentProviderRoute().getId())
                .collect(Collectors.toSet());
    }
}
