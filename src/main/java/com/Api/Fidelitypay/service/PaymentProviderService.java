package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.PaymentProviderRequest;
import com.Api.Fidelitypay.controller.dto.PaymentProviderResponse;
import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteRequest;
import com.Api.Fidelitypay.controller.dto.PaymentProviderRouteResponse;
import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import com.Api.Fidelitypay.model.PaymentProvider;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.PaymentProviderRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentProviderService {

    private final PaymentProviderRepository providerRepository;
    private final PaymentProviderRouteRepository routeRepository;

    @Transactional(readOnly = true)
    public List<PaymentProviderResponse> listProviders(boolean activeOnly) {
        List<PaymentProvider> providers = activeOnly
                ? providerRepository.findByStatusOrderByDisplayNameAsc(PaymentProviderStatus.ACTIVE)
                : providerRepository.findAll();
        return providers.stream().map(this::toProviderResponse).toList();
    }

    @Transactional
    public PaymentProviderResponse createProvider(PaymentProviderRequest request) {
        PaymentProvider provider = new PaymentProvider();
        applyProvider(provider, request);
        return toProviderResponse(providerRepository.save(provider));
    }

    @Transactional
    public PaymentProviderResponse updateProvider(Long id, PaymentProviderRequest request) {
        PaymentProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found"));
        applyProvider(provider, request);
        return toProviderResponse(providerRepository.save(provider));
    }

    @Transactional
    public PaymentProviderResponse setProviderStatus(Long id, PaymentProviderStatus status) {
        PaymentProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found"));
        provider.setStatus(status == null ? PaymentProviderStatus.ACTIVE : status);
        return toProviderResponse(providerRepository.save(provider));
    }

    @Transactional
    public void deleteProvider(Long id) {
        providerRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PaymentProviderRouteResponse> listRoutes() {
        return routeRepository.findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(
                        com.Api.Fidelitypay.enums.PaymentDirection.PAYIN)
                .stream()
                .map(route -> toRouteResponse(route, null))
                .toList();
    }

    @Transactional
    public PaymentProviderRouteResponse createRoute(PaymentProviderRouteRequest request) {
        PaymentProviderRoute route = new PaymentProviderRoute();
        applyRoute(route, request);
        return toRouteResponse(routeRepository.save(route), null);
    }

    @Transactional
    public PaymentProviderRouteResponse updateRoute(Long id, PaymentProviderRouteRequest request) {
        PaymentProviderRoute route = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider route not found"));
        applyRoute(route, request);
        return toRouteResponse(routeRepository.save(route), null);
    }

    @Transactional
    public PaymentProviderRouteResponse setRouteEnabled(Long id, boolean enabled) {
        PaymentProviderRoute route = routeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider route not found"));
        route.setEnabled(enabled);
        return toRouteResponse(routeRepository.save(route), null);
    }

    @Transactional
    public void deleteRoute(Long id) {
        routeRepository.deleteById(id);
    }

    public PaymentProviderResponse toProviderResponse(PaymentProvider provider) {
        return PaymentProviderResponse.builder()
                .id(provider.getId())
                .code(provider.getCode())
                .displayName(provider.getDisplayName())
                .status(provider.getStatus())
                .credentialSchema(provider.getCredentialSchema())
                .build();
    }

    public PaymentProviderRouteResponse toRouteResponse(PaymentProviderRoute route, Boolean merchantEnabled) {
        boolean effectiveEnabled = route.isEnabled()
                && route.isObservedUp()
                && route.getProvider().getStatus() == PaymentProviderStatus.ACTIVE
                && (merchantEnabled == null || merchantEnabled);
        return PaymentProviderRouteResponse.builder()
                .routeId(route.getId())
                .providerId(route.getProvider().getId())
                .providerCode(route.getProvider().getCode())
                .providerDisplayName(route.getProvider().getDisplayName())
                .direction(route.getDirection())
                .country(route.getCountry())
                .operator(route.getOperator())
                .flowType(route.getFlowType())
                .environment(route.getEnvironment())
                .providerChannel(route.getProviderChannel())
                .priority(route.getPriority())
                .platformEnabled(route.isEnabled())
                .observedUp(route.isObservedUp())
                .merchantEnabled(merchantEnabled)
                .effectiveEnabled(effectiveEnabled)
                .build();
    }

    private void applyProvider(PaymentProvider provider, PaymentProviderRequest request) {
        provider.setCode(request.getCode());
        provider.setDisplayName(request.getDisplayName());
        provider.setStatus(request.getStatus() == null ? PaymentProviderStatus.ACTIVE : request.getStatus());
        provider.setCredentialSchema(request.getCredentialSchema());
    }

    private void applyRoute(PaymentProviderRoute route, PaymentProviderRouteRequest request) {
        PaymentProvider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment provider not found"));
        route.setProvider(provider);
        route.setDirection(request.getDirection());
        route.setCountry(request.getCountry());
        route.setOperator(request.getOperator());
        route.setFlowType(request.getFlowType());
        route.setEnvironment(request.getEnvironment() == null || request.getEnvironment().isBlank()
                ? "LIVE"
                : request.getEnvironment());
        route.setProviderChannel(request.getProviderChannel());
        route.setEnabled(request.isEnabled());
        route.setObservedUp(request.isObservedUp());
        route.setPriority(request.getPriority());
    }
}
