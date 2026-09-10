package com.portfolio.cvanalyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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

    @Bean
    public OpenAPI cvAnalyzerOpenAPI() {
        return new OpenAPI()
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
