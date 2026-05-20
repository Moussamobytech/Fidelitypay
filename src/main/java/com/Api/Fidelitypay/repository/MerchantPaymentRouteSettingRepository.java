package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.MerchantPaymentRouteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantPaymentRouteSettingRepository extends JpaRepository<MerchantPaymentRouteSetting, Long> {
    List<MerchantPaymentRouteSetting> findByUserId(String userId);
    List<MerchantPaymentRouteSetting> findByUserIdAndEnabledFalse(String userId);
    Optional<MerchantPaymentRouteSetting> findByUserIdAndPaymentProviderRouteId(String userId, Long paymentProviderRouteId);
}
