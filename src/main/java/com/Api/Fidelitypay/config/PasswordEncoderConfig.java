package com.Api.Fidelitypay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuration for password encoding
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt password encoder for hashing API keys and passwords
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength of 12 for good security
    }
}
