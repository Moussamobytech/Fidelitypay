package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.MerchantProviderAccount;
import com.Api.Fidelitypay.model.PaymentProvider;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentRouteServiceTest {

    @Test
    void findAvailablePayIn_filtersRoutesByConfiguredEnvironmentCapability() {
        PaymentProviderRouteRepository routeRepository = mock(PaymentProviderRouteRepository.class);
        MerchantPaymentRouteSettingService settingService = mock(MerchantPaymentRouteSettingService.class);
        MerchantProviderAccountService accountService = mock(MerchantProviderAccountService.class);
        PaymentRouteService service = new PaymentRouteService(routeRepository, settingService, accountService);

        PaymentProviderRoute liveOnly = route(1L, true, false);
        PaymentProviderRoute sandboxOnly = route(2L, false, true);
        when(routeRepository.findAvailable(PaymentDirection.PAYIN, "SN", "WAVE"))
                .thenReturn(List.of(liveOnly, sandboxOnly));
        when(settingService.disabledRouteIdsForMerchant("merchant-1")).thenReturn(Set.of());
        when(settingService.routePrioritiesForMerchant("merchant-1")).thenReturn(Map.of());
        when(accountService.getEnabledAccount("merchant-1", 10L, "LIVE"))
                .thenReturn(new MerchantProviderAccount());
        when(accountService.getEnabledAccount("merchant-1", 10L, "SANDBOX"))
                .thenReturn(new MerchantProviderAccount());

        assertEquals(List.of(liveOnly), service.findAvailablePayIn("sn", "wave", "live", "merchant-1"));
        assertEquals(List.of(sandboxOnly), service.findAvailablePayIn("sn", "wave", "sandbox", "merchant-1"));
    }

    private PaymentProviderRoute route(Long id, boolean liveEnabled, boolean sandboxEnabled) {
        PaymentProvider provider = new PaymentProvider();
        provider.setId(10L);
        PaymentProviderRoute route = new PaymentProviderRoute();
        route.setId(id);
        route.setProvider(provider);
        route.setLiveEnabled(liveEnabled);
        route.setSandboxEnabled(sandboxEnabled);
        route.setPriority(10);
        return route;
    }
}
