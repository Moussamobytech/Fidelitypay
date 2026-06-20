package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.ApiKeyResponse;
import com.Api.Fidelitypay.controller.dto.CreateApiKeyRequest;
import com.Api.Fidelitypay.controller.dto.UpdateApiKeyRequest;
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
     * Get active API keys for a user without complete key values.
     */
    public List<ApiKeyResponse> getUserApiKeys(String userId) {
        List<ApiKey> keys = apiKeyRepository.findByUserIdAndIsActive(userId, true);
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
     * Create one opaque API key. Its lookup identifier is stored in plain text and
     * its secret part is hashed. The complete key is returned only once.
     */
    @Transactional
    public ApiKeyResponse createApiKey(String userId, CreateApiKeyRequest request) {
        // Generate unique keys
        String keyIdentifier = generateKeyIdentifier();
        String keySecret = generateKeySecret();

        // Ensure uniqueness
        while (apiKeyRepository.existsByPublicKey(keyIdentifier)) {
            keyIdentifier = generateKeyIdentifier();
        }

        // Hash the secret key for storage
        String secretKeyHash = passwordEncoder.encode(keySecret);

        // Extract last 4 characters for display hint
        String secretKeyHint = keySecret.substring(keySecret.length() - 4);

        // Create and save the API key
        ApiKey apiKey = ApiKey.builder()
                .userId(userId)
                .name(request.getName())
                .publicKey(keyIdentifier)
                .secretKeyHash(secretKeyHash)
                .secretKeyHint(secretKeyHint)
                .environment("GLOBAL")
                .isActive(true)
                .build();

        ApiKey savedKey = apiKeyRepository.save(apiKey);

        log.info("✅ Created new API key for user {}", userId);

        return toResponseWithKey(savedKey, formatApiKey(keyIdentifier, keySecret));
    }

    /**
     * Rename an active API key owned by a user.
     */
    @Transactional
    public ApiKeyResponse renameApiKey(String userId, String keyId, UpdateApiKeyRequest request) {
        ApiKey apiKey = findOwnedApiKey(userId, keyId);

        if (!apiKey.isActive()) {
            throw new IllegalArgumentException("API key not found");
        }

        apiKey.setName(request.getName().trim());
        ApiKey savedKey = apiKeyRepository.save(apiKey);

        log.info("Renamed API key {} for user {}", keyId, userId);
        return toResponseWithoutSecret(savedKey);
    }

    /**
     * Delete a merchant key from the merchant experience.
     *
     * The key is kept inactive internally so past payments and audits keep their key
     * reference, but it disappears from merchant key listings and cannot
     * authenticate.
     */
    @Transactional
    public void deleteApiKey(String userId, String keyId) {
        ApiKey apiKey = findOwnedApiKey(userId, keyId);
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);

        log.info("Deleted API key {} for user {}", keyId, userId);
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
     * Validate an API key (used in API authentication)
     */
    public boolean validateApiKey(String rawApiKey) {
        return authenticateApiKey(rawApiKey).isPresent();
    }

    public java.util.Optional<ApiKey> authenticateApiKey(String rawApiKey) {
        ParsedApiKey parsed = parseApiKey(rawApiKey);
        if (parsed == null) {
            return java.util.Optional.empty();
        }
        return apiKeyRepository.findByPublicKeyAndIsActive(parsed.identifier(), true)
                .filter(apiKey -> apiKey.getExpiresAt() == null || apiKey.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(apiKey -> passwordEncoder.matches(parsed.secret(), apiKey.getSecretKeyHash()));
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
     * Generate the lookup portion of an API key.
     */
    private String generateKeyIdentifier() {
        return "fp_" + generateRandomString(32);
    }

    /**
     * Generate the secret portion of an API key.
     */
    private String generateKeySecret() {
        return generateRandomString(48);
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

    private ApiKey findOwnedApiKey(String userId, String keyId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        if (userId != null && !apiKey.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: You don't own this API key");
        }

        return apiKey;
    }

    /**
     * Convert an entity to a masked response.
     */
    private ApiKeyResponse toResponseWithoutSecret(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .apiKey(null)
                .apiKeyMasked(maskApiKey(apiKey))
                .isActive(apiKey.isActive())
                .createdAt(apiKey.getCreatedAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .lastUsedIp(apiKey.getLastUsedIp())
                .expiresAt(apiKey.getExpiresAt())
                // userFullName is usually set by the calling method if needed for admin
                .build();
    }

    /**
     * Include the complete API key only in the creation response.
     */
    private ApiKeyResponse toResponseWithKey(ApiKey apiKey, String rawApiKey) {
        ApiKeyResponse response = toResponseWithoutSecret(apiKey);
        response.setApiKey(rawApiKey);
        return response;
    }

    /**
     * Mask an API key for later display.
     */
    private String maskApiKey(ApiKey apiKey) {
        return apiKey.getPublicKey() + ".****" + apiKey.getSecretKeyHint();
    }

    private String formatApiKey(String identifier, String secret) {
        return identifier + "." + secret;
    }

    private ParsedApiKey parseApiKey(String rawApiKey) {
        if (rawApiKey == null) {
            return null;
        }
        String value = rawApiKey.trim();
        int separator = value.indexOf('.');
        if (separator <= 3 || separator == value.length() - 1) {
            return null;
        }
        String identifier = value.substring(0, separator);
        String secret = value.substring(separator + 1);
        if (!identifier.startsWith("fp_") || secret.isBlank()) {
            return null;
        }
        return new ParsedApiKey(identifier, secret);
    }

    private record ParsedApiKey(String identifier, String secret) {}
}
