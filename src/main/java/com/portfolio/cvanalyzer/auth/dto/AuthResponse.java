package com.portfolio.cvanalyzer.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta de cadastro e de login.
 *
 * `expiraEmSegundos` vem junto para o cliente saber quando renovar sem
 * precisar abrir o token e interpretar o campo `exp` por conta propria.
 */
@Schema(description = "Token de acesso e dados do usuario autenticado")
public record AuthResponse(

        @Schema(description = "Token JWT a ser enviado no header Authorization")
        String token,

        @Schema(description = "Esquema do header Authorization", example = "Bearer")
        String tipo,

        @Schema(description = "Tempo de vida do token, em segundos", example = "3600")
        long expiraEmSegundos,

        UserResponse usuario
) {

    public static AuthResponse of(String token, long expiraEmSegundos, UserResponse usuario) {
        return new AuthResponse(token, "Bearer", expiraEmSegundos, usuario);
    }
}
