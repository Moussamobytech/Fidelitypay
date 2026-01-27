package com.Api.Fidelitypay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "monitoring")
@Data
public class MonitoringProperties {
    /** Intervalle de monitoring en millisecondes. Par défaut 5 minutes. */
    private long interval = 300000;
}
