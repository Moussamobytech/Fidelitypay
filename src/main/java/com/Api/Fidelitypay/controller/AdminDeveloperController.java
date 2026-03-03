package com.Api.Fidelitypay.controller;

import com.Api.Fidelitypay.controller.dto.ApiKeyResponse;
import com.Api.Fidelitypay.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Controller for Developer Management
 */
@RestController
@RequestMapping("/api/v1/admin/developers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeveloperController {

    private final ApiKeyService apiKeyService;

    /**
     * Get all API keys for all users
     */
    @GetMapping("/keys")
    public ResponseEntity<List<ApiKeyResponse>> getAllApiKeys() {
        log.info("🛡️ Admin fetching all API keys");
        return ResponseEntity.ok(apiKeyService.getAllApiKeys());
    }

    /**
     * Toggle API key status (Block/Activate)
     */
    @PatchMapping("/keys/{id}/status")
    public ResponseEntity<Map<String, String>> toggleKeyStatus(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", false);
        log.info("🛡️ Admin toggling API key {} status to {}", id, active);

        apiKeyService.adminToggleApiKeyStatus(id, active);

        return ResponseEntity.ok(Map.of(
                "message", active ? "Clé API activée" : "Clé API bloquée",
                "status", active ? "ACTIVE" : "BLOCKED"));
    }

    /**
     * Delete an API key
     */
    @DeleteMapping("/keys/{id}")
    public ResponseEntity<Map<String, String>> deleteKey(@PathVariable String id) {
        log.info("🛡️ Admin deleting API key {}", id);

        apiKeyService.adminDeleteApiKey(id);

        return ResponseEntity.ok(Map.of("message", "Clé API supprimée définitivement"));
    }
}
