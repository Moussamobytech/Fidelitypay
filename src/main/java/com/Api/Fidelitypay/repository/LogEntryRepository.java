package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.LogEntry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    // Pas de méthodes supplémentaires nécessaires pour le CRUD basique
}