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
    private String callbackUrl;

    @Data
    public static class Api {
        private String baseUrl;
        private String masterKey;
        private String privateKey;
        private String token;

        public void setMasterKey(String masterKey) {
            this.masterKey = (masterKey != null) ? masterKey.trim() : null;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = (privateKey != null) ? privateKey.trim() : null;
        }

        public void setToken(String token) {
            this.token = (token != null) ? token.trim() : null;
        }
    }

    @Data
    public static class Store {
        private String name;
    }
}
