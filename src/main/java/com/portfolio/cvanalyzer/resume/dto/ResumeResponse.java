package com.portfolio.cvanalyzer.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Curriculo completo, como o cliente da API o ve.
 *
 * Repare no que NAO esta aqui: `rawText`. O texto bruto do PDF pode ter
 * dezenas de milhares de caracteres e nao serve para nada no cliente —
 * so pesa a resposta. Entidade tem; DTO nao. E exatamente por isso que
 * eles sao classes diferentes.
 */
@Schema(description = "Curriculo processado")
public record ResumeResponse(

        UUID id,

        @Schema(example = "Maria Silva")
        String nome,

        @Schema(example = "maria.silva@email.com")
        String email,

        @Schema(example = "(11) 98888-7777")
        String telefone,

        @Schema(description = "Nome do arquivo enviado", example = "curriculo.pdf")
        String arquivo,

        @Schema(description = "Tamanho do arquivo em bytes", example = "48213")
        long tamanhoBytes,

        @Schema(description = "Numero de paginas do PDF", example = "2")
        int paginas,

        @Schema(description = "Tecnologias identificadas")
        Set<String> tecnologias,

        List<String> formacao,
        List<String> experiencias,
        List<String> cursos,
        List<String> certificacoes,
        List<String> idiomas,

        Instant criadoEm
) {
}
