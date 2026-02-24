package com.Api.Fidelitypay.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Registers interceptors for API request logging
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiRequestInterceptor apiRequestInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Register API request interceptor for all /api/** endpoints
        registry.addInterceptor(apiRequestInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/developer/health", // Exclude health check from logging
                        "/api/actuator/**" // Exclude actuator endpoints
                );
    }
}
