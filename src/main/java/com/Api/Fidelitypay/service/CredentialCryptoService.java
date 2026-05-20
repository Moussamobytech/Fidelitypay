package com.Api.Fidelitypay.service;

import com.Api.Fidelitypay.integration.ProviderCredentials;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CredentialCryptoService {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public CredentialCryptoService(ObjectMapper objectMapper,
            @Value("${credential.encryption.secret:${jwt.secret}}") String secret) {
        this.objectMapper = objectMapper;
        this.keySpec = new SecretKeySpec(deriveKey(secret), "AES");
    }

    public String encrypt(Map<String, String> credentials) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = objectMapper.writeValueAsBytes(credentials == null ? Map.of() : credentials);
            byte[] encrypted = cipher.doFinal(plain);
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt provider credentials", e);
        }
    }

    public ProviderCredentials decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return new ProviderCredentials(Map.of());
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encrypted);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            Map<String, String> values = objectMapper.readValue(plain, new TypeReference<>() {});
            return new ProviderCredentials(values);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt provider credentials", e);
        }
    }

    public Map<String, String> masked(String encrypted) {
        ProviderCredentials credentials = decrypt(encrypted);
        Map<String, String> masked = new LinkedHashMap<>();
        credentials.values().forEach((key, value) -> masked.put(key, mask(value)));
        return masked;
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "...." + value.substring(value.length() - 4);
    }

    private byte[] deriveKey(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest((secret == null ? "change-me" : secret).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive credential encryption key", e);
        }
    }
}
