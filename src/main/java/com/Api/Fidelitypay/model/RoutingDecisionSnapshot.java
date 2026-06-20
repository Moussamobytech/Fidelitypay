package com.Api.Fidelitypay.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "routing_decision_snapshots", indexes = {
        @Index(name = "idx_routing_snapshot_payment", columnList = "payment_id")
})
public class RoutingDecisionSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true, length = 50)
    private String paymentId;

    @Column(nullable = false, length = 3)
    private String country;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(nullable = false, length = 20)
    private String environment;

    @Column(name = "scoring_version", nullable = false, length = 30)
    private String scoringVersion;

    @Column(name = "selected_route_id")
    private Long selectedRouteId;

    @Lob
    @Column(name = "candidates_json", nullable = false, columnDefinition = "LONGTEXT")
    private String candidatesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
