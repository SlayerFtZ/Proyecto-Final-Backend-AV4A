package com.inventaria.api.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("InventarIA API")
                        .description("""
                                API REST de inventario inteligente con resumen diario por IA y escaneo de productos.
                                Proyecto Final — Tecnologías de Programación, Maestría en Sistemas Computacionales, ITS de Teziutlán.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("InventarIA")
                                .email("soporte@inventaria.mx")));
    }
}
