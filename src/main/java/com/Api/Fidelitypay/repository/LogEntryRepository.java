package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.Enum.LogStatus;
import com.Api.Fidelitypay.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    // Tous les logs pour un paiement, triés par date
    List<LogEntry> findByPaymentIdOrderByCreatedAtAsc(String paymentId);

    // Logs par route utilisée
    List<LogEntry> findByRouteUsed(String routeUsed);

    // Logs avec un statut spécifique
    List<LogEntry> findByStatus(LogStatus status);

    // Logs créés après une date donnée
    List<LogEntry> findByCreatedAtAfter(LocalDateTime since);

    // Logs par route + statut
    List<LogEntry> findByRouteUsedAndStatus(String routeUsed, LogStatus status);

    // Dernier log pour un paiement
    Optional<LogEntry> findFirstByPaymentIdOrderByCreatedAtDesc(String paymentId);

    // Logs par route sur une période
    List<LogEntry> findByRouteUsedAndCreatedAtAfter(String routeUsed, LocalDateTime since);
}
