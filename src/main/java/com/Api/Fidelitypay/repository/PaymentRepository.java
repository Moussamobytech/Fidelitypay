package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.Enum.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByOperator(String operator);

    List<Payment> findByOperatorAndCreatedAtBetween(
            String operator,
            LocalDateTime start,
            LocalDateTime end);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findByOperatorAndStatus(String operator, PaymentStatus status);

    List<Payment> findTop50ByOrderByCreatedAtDesc();

    // ✅ CORRIGÉ : paramètre operator obligatoire
    Optional<Payment> findFirstByOperatorOrderByCreatedAtDesc(String operator);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.country FROM Payment p WHERE p.country IS NOT NULL")
    List<String> findDistinctCountries();
}
