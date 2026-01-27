package com.Api.Fidelitypay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "http")
@Data
public class HttpProperties {
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
