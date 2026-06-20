package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import com.Api.Fidelitypay.repository.MerchantPaymentRouteSettingRepository;
import com.Api.Fidelitypay.repository.PaymentProviderRouteRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MerchantPaymentRouteSettingServiceTest {

    @Test
    void merchantRouteList_omitsPlatformDisabledRoutes() {
        PaymentProviderRouteRepository routeRepository = mock(PaymentProviderRouteRepository.class);
        MerchantPaymentRouteSettingRepository settingRepository = mock(MerchantPaymentRouteSettingRepository.class);
        PaymentProviderService paymentProviderService = mock(PaymentProviderService.class);
        MerchantPaymentRouteSettingService service = new MerchantPaymentRouteSettingService(
                routeRepository, settingRepository, paymentProviderService);

        PaymentProviderRoute enabled = new PaymentProviderRoute();
        enabled.setId(1L);
        enabled.setEnabled(true);
        PaymentProviderRoute disabled = new PaymentProviderRoute();
        disabled.setId(2L);
        disabled.setEnabled(false);

        when(settingRepository.findByUserId("user-1")).thenReturn(List.of());
        when(routeRepository.findByDirectionWithProviderOrderByCountryAscOperatorAscPriorityAsc(PaymentDirection.PAYIN))
                .thenReturn(List.of(enabled, disabled));

        assertEquals(1, service.listMerchantPayInRoutes("user-1").size());
        verify(paymentProviderService).toRouteResponse(eq(enabled), isNull(), isNull(), anyDouble());
        verify(paymentProviderService, never()).toRouteResponse(eq(disabled), isNull(), isNull(), anyDouble());
    }
}
