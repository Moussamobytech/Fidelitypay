package com.Api.Fidelitypay.repository;

import com.Api.Fidelitypay.model.RoutingFallbackConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutingFallbackConfigRepository extends JpaRepository<RoutingFallbackConfig, Long> {
}
