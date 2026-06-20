package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.controller.dto.ApiKeyResponse;
import com.Api.Fidelitypay.controller.dto.CreateApiKeyRequest;
import com.Api.Fidelitypay.model.ApiKey;
import com.Api.Fidelitypay.repository.ApiKeyRepository;
import com.Api.Fidelitypay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiKeyServiceTest {

    private ApiKeyRepository apiKeyRepository;
    private ApiKeyService service;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(ApiKeyRepository.class);
        service = new ApiKeyService(apiKeyRepository, mock(UserRepository.class), new BCryptPasswordEncoder());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAndAuthenticate_usesOneOpaqueKey() {
        CreateApiKeyRequest request = new CreateApiKeyRequest();
        request.setName("Production backend");

        ApiKeyResponse created = service.createApiKey("user-1", request);
        String rawKey = created.getApiKey();

        assertNotNull(rawKey);
        assertTrue(rawKey.startsWith("fp_"));
        assertTrue(rawKey.contains("."));
        assertTrue(created.getApiKeyMasked().contains(".****"));

        String identifier = rawKey.substring(0, rawKey.indexOf('.'));
        ApiKey stored = ApiKey.builder()
                .id("key-1")
                .userId("user-1")
                .name("Production backend")
                .publicKey(identifier)
                .secretKeyHash(new BCryptPasswordEncoder().encode(rawKey.substring(rawKey.indexOf('.') + 1)))
                .secretKeyHint("abcd")
                .isActive(true)
                .build();
        when(apiKeyRepository.findByPublicKeyAndIsActive(identifier, true)).thenReturn(Optional.of(stored));
        when(apiKeyRepository.findByUserIdAndIsActive("user-1", true)).thenReturn(List.of(stored));

        assertEquals("key-1", service.authenticateApiKey(rawKey).orElseThrow().getId());
        assertFalse(service.authenticateApiKey(identifier + ".wrong-secret").isPresent());
        assertFalse(service.authenticateApiKey("not-a-fidelitypay-key").isPresent());

        ApiKeyResponse listed = service.getUserApiKeys("user-1").get(0);
        assertNull(listed.getApiKey());
        assertEquals(identifier + ".****abcd", listed.getApiKeyMasked());
    }

    @Test
    void deleteKey_disablesOwnedKey() {
        ApiKey stored = ApiKey.builder()
                .id("key-1")
                .userId("user-1")
                .name("Backend")
                .publicKey("fp_identifier")
                .secretKeyHash("hash")
                .isActive(true)
                .build();
        when(apiKeyRepository.findById("key-1")).thenReturn(Optional.of(stored));

        service.deleteApiKey("user-1", "key-1");

        assertFalse(stored.isActive());
        verify(apiKeyRepository).save(stored);
    }
}
