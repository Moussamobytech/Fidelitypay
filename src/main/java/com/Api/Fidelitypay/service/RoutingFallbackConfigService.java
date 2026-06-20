package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.FallbackSettingsDto;
import com.Api.Fidelitypay.model.RoutingFallbackConfig;
import com.Api.Fidelitypay.repository.RoutingFallbackConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoutingFallbackConfigService {

    private final RoutingFallbackConfigRepository repository;

    private static final long SINGLETON_ID = 1L;

    /** Returns current config, creating a default row if none exists yet. */
    @Transactional
    public FallbackSettingsDto getSettings() {
        RoutingFallbackConfig config = repository.findById(SINGLETON_ID)
                .orElseGet(this::createDefaults);
        return toDto(config);
    }

    /** Saves (upsert) the fallback config and returns the updated state. */
    @Transactional
    public FallbackSettingsDto updateSettings(FallbackSettingsDto dto) {
        RoutingFallbackConfig config = repository.findById(SINGLETON_ID)
                .orElseGet(this::createDefaults);

        config.setAutoFallback(dto.isAutoFallback());
        config.setSmartRetry(dto.isSmartRetry());
        config.setRetryMaxAttempts(dto.getRetryMaxAttempts());
        config.setCriticalTimeoutSeconds(dto.getCriticalTimeoutSeconds());

        return toDto(repository.save(config));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RoutingFallbackConfig createDefaults() {
        RoutingFallbackConfig defaults = new RoutingFallbackConfig();
        defaults.setId(SINGLETON_ID);
        return repository.save(defaults);
    }

    private FallbackSettingsDto toDto(RoutingFallbackConfig c) {
        FallbackSettingsDto dto = new FallbackSettingsDto();
        dto.setAutoFallback(c.isAutoFallback());
        dto.setSmartRetry(c.isSmartRetry());
        dto.setRetryMaxAttempts(c.getRetryMaxAttempts());
        dto.setCriticalTimeoutSeconds(c.getCriticalTimeoutSeconds());
        return dto;
    }
}
