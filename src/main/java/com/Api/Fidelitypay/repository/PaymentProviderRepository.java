package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.enums.PaymentProviderStatus;
import com.Api.Fidelitypay.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {
    Optional<PaymentProvider> findByCode(String code);

    List<PaymentProvider> findByStatusOrderByDisplayNameAsc(PaymentProviderStatus status);
}
