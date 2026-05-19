package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.ApiKeyResponse;
import com.Api.Fidelitypay.controller.dto.CreateApiKeyRequest;
import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing API keys
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final com.Api.Fidelitypay.repository.UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Get all API keys for a user (without secret keys)
     */
    public List<ApiKeyResponse> getUserApiKeys(String userId) {
        List<ApiKey> keys = apiKeyRepository.findByUserId(userId);
        return keys.stream()
                .map(this::toResponseWithoutSecret)
                .collect(Collectors.toList());
    }

    /**
     * Get ALL API keys in the system (Admin only)
     */
    public List<ApiKeyResponse> getAllApiKeys() {
        // Fetch all users once for mapping
        List<com.Api.Fidelitypay.model.User> allUsers = userRepository.findAll();
        java.util.Map<String, String> userNames = allUsers.stream()
                .collect(Collectors.toMap(com.Api.Fidelitypay.model.User::getId,
                        com.Api.Fidelitypay.model.User::getFullName, (a, b) -> a));
        java.util.Map<String, String> userEmails = allUsers.stream()
                .collect(Collectors.toMap(com.Api.Fidelitypay.model.User::getId,
                        com.Api.Fidelitypay.model.User::getEmail, (a, b) -> a));

        return apiKeyRepository.findAll().stream()
                .map(key -> {
                    ApiKeyResponse resp = toResponseWithoutSecret(key);
                    resp.setUserFullName(userNames.getOrDefault(key.getUserId(), "Utilisateur inconnu"));
                    resp.setUserEmail(userEmails.getOrDefault(key.getUserId(), "N/A"));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get API keys by environment
     */
    public List<ApiKeyResponse> getUserApiKeysByEnvironment(String userId, String environment) {
        List<ApiKey> keys = apiKeyRepository.findByUserIdAndEnvironment(userId, environment);
        return keys.stream()
                .map(this::toResponseWithoutSecret)
                .collect(Collectors.toList());
    }

    /**
     * Create a new API key pair
     * IMPORTANT: The secret key is returned ONLY ONCE - it will NOT be retrievable
     * later
     */
    @Transactional
    public ApiKeyResponse createApiKey(String userId, CreateApiKeyRequest request) {
        // Generate unique keys
        String publicKey = generatePublicKey(request.getEnvironment());
        String secretKey = generateSecretKey(request.getEnvironment());

        // Ensure uniqueness
        while (apiKeyRepository.existsByPublicKey(publicKey)) {
            publicKey = generatePublicKey(request.getEnvironment());
        }

        // Hash the secret key for storage
        String secretKeyHash = passwordEncoder.encode(secretKey);

        // Extract last 4 characters for display hint
        String secretKeyHint = secretKey.substring(secretKey.length() - 4);

        // Create and save the API key
        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .name(request.getName())
                .publicKey(publicKey)
                .secretKeyHash(secretKeyHash)
                .secretKeyHint(secretKeyHint)
                .environment(request.getEnvironment())
                .isActive(true)
                .build();

        ApiKey savedKey = apiKeyRepository.save(apiKey);

        log.info("✅ Created new API key for user {} in {} environment", userId, request.getEnvironment());

        // Return response with the ACTUAL secret key (only time it's shown)
        return toResponseWithSecret(savedKey, secretKey);
    }

    /**
     * Revoke (deactivate) an API key
     */
    @Transactional
    public void revokeApiKey(String userId, String keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        // Verify ownership if userId is provided
        if (userId != null && !apiKey.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: You don't own this API key");
        }

        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        log.info("🔒 Revoked API key {} for user {}", keyId, userId != null ? userId : "ADMIN");
    }

    /**
     * Admin toggle API key status
     */
    @Transactional
    public void adminToggleApiKeyStatus(String keyId, boolean active) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        apiKey.setActive(active);
        apiKeyRepository.save(apiKey);
        log.info("🛡️ Admin set API key {} status to {}", keyId, active);
    }

    /**
     * Admin delete API key
     */
    @Transactional
    public void adminDeleteApiKey(String keyId) {
        if (!apiKeyRepository.existsById(keyId)) {
            throw new IllegalArgumentException("API key not found");
        }
        apiKeyRepository.deleteById(keyId);
        log.info("🗑️ Admin deleted API key {}", keyId);
    }

    /**
     * Rotate API keys - revoke all old keys and create new ones
     * This is a sensitive operation that should require additional confirmation
     */
    @Transactional
    public List<ApiKeyResponse> rotateApiKeys(String userId) {
        // Revoke all existing active keys
        List<ApiKey> existingKeys = apiKeyRepository.findByUserIdAndIsActive(userId, true);
        existingKeys.forEach(key -> key.setActive(false));
        apiKeyRepository.saveAll(existingKeys);

        log.warn("🔄 Rotating all API keys for user {} - {} keys revoked", userId, existingKeys.size());

        // Create new keys for each environment that had active keys
        boolean hadSandbox = existingKeys.stream().anyMatch(k -> "sandbox".equals(k.getEnvironment()));
        boolean hadLive = existingKeys.stream().anyMatch(k -> "live".equals(k.getEnvironment()));

        List<ApiKeyResponse> newKeys = new java.util.ArrayList<>();

        if (hadSandbox) {
            CreateApiKeyRequest sandboxRequest = CreateApiKeyRequest.builder()
                    .name("Rotated Sandbox Key")
                    .environment("sandbox")
                    .build();
            newKeys.add(createApiKey(userId, sandboxRequest));
        }

        if (hadLive) {
            CreateApiKeyRequest liveRequest = CreateApiKeyRequest.builder()
                    .name("Rotated Live Key")
                    .environment("live")
                    .build();
            newKeys.add(createApiKey(userId, liveRequest));
        }

        return newKeys;
    }

    /**
     * Validate an API key (used in API authentication)
     */
    public boolean validateApiKey(String publicKey, String secretKey) {
        return apiKeyRepository.findByPublicKeyAndIsActive(publicKey, true)
                .map(apiKey -> passwordEncoder.matches(secretKey, apiKey.getSecretKeyHash()))
                .orElse(false);
    }

    public java.util.Optional<ApiKey> authenticateApiKey(String publicKey, String secretKey) {
        if (publicKey == null || publicKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return java.util.Optional.empty();
        }
        return apiKeyRepository.findByPublicKeyAndIsActive(publicKey.trim(), true)
                .filter(apiKey -> apiKey.getExpiresAt() == null || apiKey.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(apiKey -> passwordEncoder.matches(secretKey.trim(), apiKey.getSecretKeyHash()));
    }

    /**
     * Update last used timestamp for an API key
     */
    @Transactional
    public void updateLastUsed(String publicKey, String ipAddress) {
        apiKeyRepository.findByPublicKey(publicKey).ifPresent(apiKey -> {
            apiKey.setLastUsedAt(LocalDateTime.now());
            apiKey.setLastUsedIp(ipAddress);
            apiKeyRepository.save(apiKey);
        });
    }

    /**
     * Generate a public key in format: pk_{env}_{random}
     */
    private String generatePublicKey(String environment) {
        String prefix = "pk_" + environment + "_";
        return prefix + generateRandomString(32);
    }

    /**
     * Generate a secret key in format: sk_{env}_{random}
     */
    private String generateSecretKey(String environment) {
        String prefix = "sk_" + environment + "_";
        return prefix + generateRandomString(48);
    }

    /**
     * Generate cryptographically secure random string
     */
    private String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes)
                .substring(0, length);
    }

    /**
     * Convert ApiKey entity to response DTO WITHOUT secret key
     */
    private ApiKeyResponse toResponseWithoutSecret(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .publicKey(apiKey.getPublicKey())
                .secretKey(null) // Never expose the secret key after creation
                .secretKeyMasked(maskSecretKey(apiKey))
                .environment(apiKey.getEnvironment())
                .isActive(apiKey.isActive())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .lastUsedIp(apiKey.getLastUsedIp())
                .expiresAt(apiKey.getExpiresAt())
                // userFullName is usually set by the calling method if needed for admin
                .build();
    }

    /**
     * Convert ApiKey entity to response DTO WITH secret key (only for creation)
     */
    private ApiKeyResponse toResponseWithSecret(ApiKey apiKey, String secretKey) {
        ApiKeyResponse response = toResponseWithoutSecret(apiKey);
        response.setSecretKey(secretKey); // Only set during creation
        return response;
    }

    /**
     * Mask secret key for display (e.g., "sk_live_****1234")
     */
    private String maskSecretKey(ApiKey apiKey) {
        String prefix = "sk_" + apiKey.getEnvironment() + "_";
        return prefix + "****" + apiKey.getSecretKeyHint();
    }
}
