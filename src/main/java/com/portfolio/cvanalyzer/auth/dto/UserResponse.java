package com.portfolio.cvanalyzer.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Usuario como o cliente o enxerga.
 *
 * Nao existe campo de senha nem de hash aqui, e essa e a razao de o DTO
 * existir: devolver a entidade AppUser direto exporia o hash em toda
 * resposta. DTO de saida e barreira, nao burocracia.
 */
@Schema(description = "Dados publicos do usuario")
public record UserResponse(

        UUID id,
        String nome,
        String email,

        @Schema(description = "Papel do usuario", example = "RECRUTADOR")
        String papel,

        Instant criadoEm
) {
}
