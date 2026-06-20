package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.MerchantApiPrincipal;
import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.model.User;
import com.Api.Fidelitypay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantApiAuthService {

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;

    public MerchantApiPrincipal authenticate(String rawApiKey, String ipAddress) {
        ApiKey apiKey = apiKeyService.authenticateApiKey(rawApiKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));
        User user = userRepository.findById(apiKey.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("API key owner not found"));
        apiKeyService.updateLastUsed(apiKey.getPublicKey(), ipAddress);
        return MerchantApiPrincipal.builder()
                .apiKey(apiKey)
                .user(user)
                .environment("LIVE")
                .initiationSource("API")
                .build();
    }
}
