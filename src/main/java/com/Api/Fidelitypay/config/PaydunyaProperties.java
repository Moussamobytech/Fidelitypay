package com.Api.Fidelitypay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "paydunya")
@Data
public class PaydunyaProperties {
    private Api api = new Api();
    private Store store = new Store();

    @Data
    public static class Api {
        private String baseUrl;
        private String masterKey;
        private String privateKey;
        private String token;
    }

    @Data
    public static class Store {
        private String name;
    }
}
