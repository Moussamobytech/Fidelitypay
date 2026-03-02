package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.Payment;
import com.Api.Fidelitypay.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") String userId);

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

    // Ajoutez ces deux méthodes
    List<Payment> findByUsedFallbackTrueOrderByCreatedAtDesc();

    // Ajoutez cette méthode
    List<Payment> findByUsedFallbackTrueAndFallbackReasonContainingIgnoreCaseOrderByCreatedAtDesc(String reason);

    // Optionnel: pour les requêtes plus complexes
    @Query("SELECT p.fallbackReason, COUNT(p) FROM Payment p WHERE p.usedFallback = true AND p.fallbackReason IS NOT NULL GROUP BY p.fallbackReason")
    List<Object[]> countFallbacksByReason();

    long countByUsedFallbackTrue();

    // ✅ CORRIGÉ : paramètre operator obligatoire
    Optional<Payment> findFirstByOperatorOrderByCreatedAtDesc(String operator);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.country FROM Payment p WHERE p.country IS NOT NULL")
    List<String> findDistinctCountries();
}
