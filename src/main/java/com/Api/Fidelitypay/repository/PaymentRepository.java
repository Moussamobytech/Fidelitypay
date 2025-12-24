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

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByOperator(String operator);

    List<Payment> findByOperatorAndCreatedAtBetween(
            String operator,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Payment> findByOperatorAndStatus(String operator, PaymentStatus status);

    List<Payment> findTop50ByOrderByCreatedAtDesc();

    // ✅ CORRIGÉ : paramètre operator obligatoire
    Optional<Payment> findFirstByOperatorOrderByCreatedAtDesc(String operator);
}
