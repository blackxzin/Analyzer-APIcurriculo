package com.portfolio.cvanalyzer.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO de SAIDA do endpoint de health.
 *
 * Sempre um tipo proprio, nunca a entidade e nunca Map<String,Object>:
 *  - o contrato fica explicito e documentado no Swagger;
 *  - o compilador avisa se alguem quebrar o formato.
 */
@Schema(description = "Estado atual da aplicacao")
public record HealthResponse(

        @Schema(description = "Estado do servico", example = "UP")
        String status,

        @Schema(description = "Nome da aplicacao", example = "cv-analyzer-api")
        String application,

        @Schema(description = "Versao da aplicacao", example = "0.0.1-SNAPSHOT")
        String version,

        @Schema(description = "Momento da checagem, em UTC")
        Instant checkedAt
) {
}
