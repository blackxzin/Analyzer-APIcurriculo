package com.portfolio.cvanalyzer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Limites do upload de curriculo, lidos de `app.resume` no application.yml.
 *
 * Por que @ConfigurationProperties e nao @Value espalhado?
 *  - agrupa configuracao relacionada num objeto tipado;
 *  - @Validated faz a aplicacao NAO subir com configuracao invalida
 *    (limite zero, lista de tipos vazia), em vez de falhar so no primeiro
 *    upload em producao;
 *  - `record` deixa tudo imutavel.
 */
@Validated
@ConfigurationProperties(prefix = "app.resume")
public record ResumeProperties(

        @Min(1)
        long maxFileSizeBytes,

        @Min(1)
        int maxPages,

        @Min(1)
        int minTextLength,

        @NotEmpty
        List<String> allowedContentTypes
) {
}
