package com.portfolio.cvanalyzer.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resultado da analise.
 *
 * Os nomes dos campos seguem exatamente o contrato pedido:
 * compatibilidade, pontosFortes, requisitosAusentes, recomendacoes.
 */
@Schema(description = "Resultado da comparacao entre curriculo e vaga")
public record AnalysisResponse(

        UUID id,
        UUID curriculoId,
        UUID vagaId,

        @Schema(description = "Nome do candidato, quando identificado")
        String candidato,

        @Schema(description = "Titulo da vaga analisada", example = "Desenvolvedor Java Backend")
        String vaga,

        @Schema(description = "Percentual de compatibilidade, de 0 a 100", example = "78")
        int compatibilidade,

        @Schema(description = "Requisitos da vaga que o candidato ja possui",
                example = "[\"Java\", \"SQL\", \"Git\"]")
        List<String> pontosFortes,

        @Schema(description = "Requisitos da vaga que o candidato nao possui",
                example = "[\"Spring Boot\", \"Docker\"]")
        List<String> requisitosAusentes,

        @Schema(description = "Sugestoes para aumentar a compatibilidade",
                example = "[\"Estudar Spring Boot\", \"Criar um projeto utilizando Docker\"]")
        List<String> recomendacoes,

        @Schema(description = "Total de requisitos avaliados", example = "9")
        int totalRequisitos,

        Instant analisadoEm
) {
}
