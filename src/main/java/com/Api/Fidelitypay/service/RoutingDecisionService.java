package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.RoutingPreviewResponse;
import com.Api.Fidelitypay.model.RoutingDecisionSnapshot;
import com.Api.Fidelitypay.repository.RoutingDecisionSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutingDecisionService {
    private final RoutingDecisionSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(String paymentId, RoutingPreviewResponse decision) {
        RoutingDecisionSnapshot snapshot = new RoutingDecisionSnapshot();
        snapshot.setPaymentId(paymentId);
        snapshot.setCountry(decision.getCountry());
        snapshot.setOperator(decision.getOperator());
        snapshot.setEnvironment(decision.getEnvironment());
        snapshot.setScoringVersion(decision.getScoringVersion());
        snapshot.setSelectedRouteId(decision.getSelected() == null ? null : decision.getSelected().getRouteId());
        snapshot.setCreatedAt(decision.getEvaluatedAt());
        try {
            snapshot.setCandidatesJson(objectMapper.writeValueAsString(decision.getCandidates()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize routing decision", exception);
        }
        repository.save(snapshot);
    }
}
