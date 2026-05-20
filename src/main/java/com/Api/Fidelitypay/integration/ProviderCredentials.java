package com.Api.Fidelitypay.integration;

import java.util.Map;

public record ProviderCredentials(Map<String, String> values) {
    public String get(String key) {
        return values == null ? null : values.get(key);
    }

    public String publicKey() {
        return first("publicKey", "public_key", "cleApblic");
    }

    public String privateKey() {
        return first("privateKey", "private_key", "cleApr");
    }

    public String token() {
        return first("token", "accessToken", "cleAtoken");
    }

    public String masterKey() {
        return first("masterKey", "master_key");
    }

    public String secretKey() {
        return first("secretKey", "secret_key");
    }

    private String first(String... keys) {
        if (values == null) {
            return null;
        }
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
