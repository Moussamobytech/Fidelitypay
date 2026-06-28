package com.Api.Fidelitypay.service.routing;

import com.Api.Fidelitypay.enums.FeeType;
import com.Api.Fidelitypay.model.PaymentProviderRoute;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RouteFeeCalculator {

    public boolean supportsAmount(PaymentProviderRoute route, Double amount) {
        if (amount == null) {
            return true;
        }
        return amount >= route.getMinAmount()
                && (route.getMaxAmount() == null || amount <= route.getMaxAmount());
    }

    public double estimateFee(PaymentProviderRoute route, double amount) {
        double fee = route.getFeeType() == FeeType.FIXED
                ? route.getFixedFee()
                : (amount * route.getFeeRate()) / 100.0;
        return BigDecimal.valueOf(Math.max(0.0, fee))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
