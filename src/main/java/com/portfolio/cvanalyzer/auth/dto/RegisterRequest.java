package com.portfolio.cvanalyzer.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro de um novo usuario.
 *
 * O tamanho minimo da senha e validado aqui, na borda: um dado invalido
 * nunca deve chegar ao service. Oito caracteres e o piso do NIST SP 800-63B.
 */
@Schema(description = "Cadastro de um novo usuario")
public record RegisterRequest(

        @NotBlank(message = "o nome e obrigatorio")
        @Size(max = 120, message = "o nome deve ter no maximo 120 caracteres")
        @Schema(example = "Ana Souza")
        String nome,

        @NotBlank(message = "o e-mail e obrigatorio")
        @Email(message = "informe um e-mail valido")
        @Size(max = 180, message = "o e-mail deve ter no maximo 180 caracteres")
        @Schema(example = "ana@empresa.com")
        String email,

        @NotBlank(message = "a senha e obrigatoria")
        @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
        @Schema(example = "senha-forte-123", minLength = 8)
        String senha
) {
}
