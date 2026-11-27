package com.inventaria.api.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InventarIA API")
                        .description("""
                                API REST de inventario inteligente con resumen diario por IA y escaneo de productos.
                                Proyecto Final — Tecnologías de Programación, Maestría en Sistemas Computacionales, ITS de Teziutlán.

                                Autenticación: obtén un JWT con POST /api/auth/google y pégalo en el botón "Authorize".
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("InventarIA")
                                .email("soporte@inventaria.mx")))
                // Botón "Authorize" de Swagger: envía Authorization: Bearer <jwt>
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
