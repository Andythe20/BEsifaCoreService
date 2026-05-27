package com.sifa.core_sifa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfing {
    @Bean
    public OpenAPI coreOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        // Configura la URL base a través de la cual Swagger ejecutará los "Try it out"
        Server gatewayServer = new Server()
                .url("http://localhost:9000") // URL pública del API Gateway
                .description("API Gateway Perimetral (Entorno de Desarrollo)");

        return new OpenAPI()
                .info(new Info()
                        .title("Módulo de Negocio (Core) - SIFA")
                        .version("1.0.0")
                        .description("Motor principal de reglas de negocio del ecosistema SIFA. Encargado de procesar y tipificar infracciones de tránsito en terreno, gestionar la telemetría GPS de los fiscalizadores y administrar el ciclo de vida de las citaciones del Juzgado de Policía Local (JPL)."))
                .servers(List.of(gatewayServer))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa el Token JWT válido emitido por el servicio de Autenticación para interactuar con las APIs del Core.")));
    }
}
