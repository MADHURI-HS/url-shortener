package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()

                .info(
                        new Info()
                                .title("URL Shortener REST API")
                                .version("1.0")
                                .description("""
                                    A URL shortener built with Spring Boot.

                                    Features:
                                    • JWT Authentication
                                    • Redis Caching
                                    • Bucket4j Rate Limiting
                                    • MySQL
                                    • Swagger Documentation
                                    """)
                )

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                )

                .schemaRequirement(
                        securitySchemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );
    }
}