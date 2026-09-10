package com.portfolio.cvanalyzer.job.dto;

import com.portfolio.cvanalyzer.job.Seniority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resumo de uma vaga, para listagem")
public record JobSummaryResponse(
        UUID id,
        String titulo,
        String empresa,
        Seniority senioridade,
        int totalRequisitos,
        Instant criadoEm
) {
}
