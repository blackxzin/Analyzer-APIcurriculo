package com.portfolio.cvanalyzer.job.dto;

import com.portfolio.cvanalyzer.job.Seniority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Vaga cadastrada")
public record JobResponse(
        UUID id,
        String titulo,
        String empresa,
        Seniority senioridade,
        String descricao,
        List<String> requisitosObrigatorios,
        List<String> requisitosDesejaveis,
        Instant criadoEm,
        Instant atualizadoEm
) {
}
