package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.enums.PaymentDirection;
import com.Api.Fidelitypay.model.PaymentRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRouteRepository extends JpaRepository<PaymentRoute, Long> {
    List<PaymentRoute> findByDirectionAndEnabledTrueAndObservedUpTrueAndCountryAndOperatorAndEnvironmentOrderByPriorityAsc(
            PaymentDirection direction,
            String country,
            String operator,
            String environment);

    List<PaymentRoute> findByDirectionAndCountryAndOperatorOrderByPriorityAsc(
            PaymentDirection direction,
            String country,
            String operator);
}
