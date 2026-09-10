package com.portfolio.cvanalyzer.job.dto;

import com.portfolio.cvanalyzer.job.Seniority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Dados de entrada para cadastrar ou atualizar uma vaga.
 *
 * Validacao declarativa com Bean Validation: as regras ficam ao lado do
 * campo, o Spring as aplica antes de chamar o service e o
 * GlobalExceptionHandler transforma a falha em 400 campo a campo.
 * O service nunca recebe dado invalido.
 *
 * Se as duas listas de requisitos vierem vazias, a aplicacao extrai os
 * requisitos automaticamente da descricao.
 */
@Schema(description = "Dados de uma vaga")
public record JobRequest(

        @NotBlank(message = "O titulo da vaga e obrigatorio")
        @Size(max = 150, message = "O titulo deve ter no maximo 150 caracteres")
        @Schema(example = "Desenvolvedor Java Backend")
        String titulo,

        @NotBlank(message = "A empresa e obrigatoria")
        @Size(max = 150, message = "A empresa deve ter no maximo 150 caracteres")
        @Schema(example = "Acme Tecnologia")
        String empresa,

        @NotNull(message = "A senioridade e obrigatoria")
        @Schema(example = "PLENO")
        Seniority senioridade,

        @NotBlank(message = "A descricao da vaga e obrigatoria")
        @Size(min = 20, max = 20000, message = "A descricao deve ter entre 20 e 20000 caracteres")
        @Schema(example = "Buscamos pessoa desenvolvedora com Java, Spring Boot, Docker e PostgreSQL.")
        String descricao,

        @Schema(description = "Requisitos obrigatorios. Se vazio, sao extraidos da descricao.",
                example = "[\"Java\", \"Spring Boot\"]")
        List<@NotBlank @Size(max = 80) String> requisitosObrigatorios,

        @Schema(description = "Requisitos desejaveis (diferenciais)",
                example = "[\"Kubernetes\"]")
        List<@NotBlank @Size(max = 80) String> requisitosDesejaveis
) {

    /** Normaliza null para lista vazia: o service nunca precisa checar null. */
    public JobRequest {
        requisitosObrigatorios = requisitosObrigatorios == null ? List.of() : List.copyOf(requisitosObrigatorios);
        requisitosDesejaveis = requisitosDesejaveis == null ? List.of() : List.copyOf(requisitosDesejaveis);
    }
}
