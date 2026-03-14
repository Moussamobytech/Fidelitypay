package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.model.LogEntry;
import com.Api.Fidelitypay.model.Route;
import com.Api.Fidelitypay.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @PostMapping("/check")
    public ResponseEntity<List<Route>> triggerMonitoring() {
        monitoringService.checkRoutes();
        return ResponseEntity.ok(monitoringService.getAllRoutes());
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Route>> getRoutes() {
        return ResponseEntity.ok(monitoringService.getAllRoutes());
    }

    @PostMapping("/routes/toggle")
    public ResponseEntity<Route> toggleRoute(@RequestBody Map<String, Object> payload) {
        Long id = Long.valueOf(payload.get("id").toString());
        boolean enabled = (boolean) payload.get("enabled");
        return ResponseEntity.ok(monitoringService.toggleRoute(id, enabled));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<LogEntry>> getLogs() {
        return ResponseEntity.ok(monitoringService.getAllLogs());
    }
}
