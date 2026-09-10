package com.portfolio.cvanalyzer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentacao OpenAPI (Swagger).
 *
 * @Configuration = classe que produz beans.
 * @Bean          = o objeto devolvido entra no container do Spring e o
 *                  springdoc o encontra automaticamente.
 *
 * Fica em `config/` porque nao e regra de negocio nem acesso a dado:
 * e configuracao de infraestrutura.
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearer-jwt";

    @Bean
    public OpenAPI cvAnalyzerOpenAPI() {
        return new OpenAPI()
                // Declarar o esquema faz aparecer o botao "Authorize" no Swagger UI:
                // cola-se o token uma vez e todas as chamadas passam a envia-lo.
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido por /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .info(new Info()
                        .title("CV Analyzer API")
                        .description("""
                                API para upload e analise de curriculos em PDF,
                                cadastro de vagas e calculo de compatibilidade
                                entre candidato e vaga.
                                """)
                        .version("v1")
                        .contact(new Contact().name("Portfolio"))
                        .license(new License().name("MIT")));
    }
}
