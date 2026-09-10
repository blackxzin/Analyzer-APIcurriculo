package com.portfolio.cvanalyzer.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Item do historico de analises")
public record AnalysisSummaryResponse(
        UUID id,
        UUID curriculoId,
        String candidato,
        UUID vagaId,
        String vaga,
        String empresa,
        int compatibilidade,
        int requisitosAtendidos,
        int totalRequisitos,
        Instant analisadoEm
) {
}
