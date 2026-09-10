package com.portfolio.cvanalyzer.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Versao enxuta para listagem.
 *
 * Listar 50 curriculos com todas as secoes de cada um seria pesado e
 * inutil na tela de lista. DTO menor = resposta menor = menos consulta.
 */
@Schema(description = "Resumo de um curriculo, para listagem")
public record ResumeSummaryResponse(
        UUID id,
        String nome,
        String email,
        String arquivo,
        int paginas,
        Instant criadoEm
) {
}
