package com.portfolio.cvanalyzer.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso")
public record LoginRequest(

        @NotBlank(message = "o e-mail e obrigatorio")
        @Schema(example = "ana@empresa.com")
        String email,

        @NotBlank(message = "a senha e obrigatoria")
        @Schema(example = "senha-forte-123")
        String senha
) {
}
