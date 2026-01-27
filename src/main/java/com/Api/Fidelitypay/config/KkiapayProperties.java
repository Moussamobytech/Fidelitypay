package com.Api.Fidelitypay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kkiapay")
@Data
public class KkiapayProperties {
    private Api api = new Api();
    private String storeName;
    private String callbackUrl;

    @Data
    public static class Api {
        private String baseUrl;
        private String publicKey;
        private String privateKey;
        private String secretKey;
    }
}
