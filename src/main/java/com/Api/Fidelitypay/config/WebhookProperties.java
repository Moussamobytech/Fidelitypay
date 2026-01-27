package com.Api.Fidelitypay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "webhook")
@Data
public class WebhookProperties {
    /** URL du webhook pour notifier les systèmes externes après un succès. */
    private String url;
}
