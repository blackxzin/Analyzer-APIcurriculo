package com.portfolio.cvanalyzer.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Solicitacao de analise de compatibilidade")
public record AnalysisRequest(

        @NotNull(message = "O identificador do curriculo e obrigatorio")
        @Schema(description = "Identificador do curriculo ja enviado")
        UUID curriculoId,

        @NotNull(message = "O identificador da vaga e obrigatorio")
        @Schema(description = "Identificador da vaga ja cadastrada")
        UUID vagaId
) {
}
