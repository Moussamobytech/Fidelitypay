package com.Api.Fidelitypay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger configuration.
 *
 * Produces two separate API groups:
 *  - "merchant-sdk"  → public endpoints consumed by merchant integrations / SDKs
 *  - "admin"         → internal dashboard + admin endpoints
 *
 * Access the UI at: http://localhost:8060/swagger-ui.html
 * Raw spec JSON  at: http://localhost:8060/v3/api-docs
 * SDK group spec  at: http://localhost:8060/v3/api-docs/merchant-sdk
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String API_KEY_SCHEME = "apiKeyAuth";

    @Value("${server.port:8060}")
    private String serverPort;

    // -------------------------------------------------------------------------
    // Global OpenAPI definition
    // -------------------------------------------------------------------------

    @Bean
    public OpenAPI fidelityPayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FidelityPay API")
                        .description("""
                                REST API for the FidelityPay payment gateway.

                                ## Authentication
                                - **Merchant endpoints** (`/api/v1/payments/**`): use the `X-API-Key` header with your secret API key.
                                - **Dashboard / Admin endpoints**: use a JWT Bearer token obtained from `POST /api/v1/auth/login`.

                                ## SDK Generation
                                The OpenAPI spec can be downloaded at `/v3/api-docs/merchant-sdk` and used with
                                `openapi-generator-cli` to generate typed clients for TypeScript, Java, Python, etc.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Aplika / FidelityPay Team")
                                .email("dev@fidelity-market.com"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local development"),
                        new Server().url("https://dev.pay.fidelity-market.com").description("Staging")))
                // Security schemes available globally
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from POST /api/v1/auth/login"))
                        .addSecuritySchemes(API_KEY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("Merchant API key generated in the developer dashboard")));
    }

    // -------------------------------------------------------------------------
    // Group 1 — Public Merchant SDK
    //   Scope: endpoints a third-party application would call directly.
    //   This group is used by the SDK generator.
    // -------------------------------------------------------------------------

    @Bean
    public GroupedOpenApi merchantSdkApi() {
        return GroupedOpenApi.builder()
                .group("merchant-sdk")
                .displayName("Merchant SDK (public)")
                .pathsToMatch("/api/v1/payments/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title("FidelityPay Merchant SDK API")
                                .description("""
                                        Public endpoints for third-party merchant integrations.
                                        Authenticate every request with the `X-API-Key` header.
                                        """)
                                .version("1.0.0"))
                        .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME)))
                .build();
    }

    // -------------------------------------------------------------------------
    // Group 2 — Developer Portal (JWT-authenticated)
    // -------------------------------------------------------------------------

    @Bean
    public GroupedOpenApi developerApi() {
        return GroupedOpenApi.builder()
                .group("developer-portal")
                .displayName("Developer Portal")
                .pathsToMatch("/api/v1/developer/**", "/api/v1/auth/**",
                        "/api/payment-options", "/api/payments/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title("FidelityPay Developer Portal API")
                                .description("API keys, webhooks, routing, payment testing – JWT-authenticated.")
                                .version("1.0.0"))
                        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME)))
                .build();
    }

    // -------------------------------------------------------------------------
    // Group 3 — Admin (JWT-authenticated, ADMIN role required)
    // -------------------------------------------------------------------------

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin")
                .pathsToMatch("/api/v1/admin/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(new Info()
                                .title("FidelityPay Admin API")
                                .description("Internal admin endpoints. Requires ADMIN role.")
                                .version("1.0.0"))
                        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME)))
                .build();
    }
}
