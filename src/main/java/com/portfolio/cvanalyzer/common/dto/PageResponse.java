package com.portfolio.cvanalyzer.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Pagina de resultados no nosso formato.
 *
 * Por que nao devolver o `Page` do Spring direto? Porque o JSON dele
 * carrega detalhes internos do framework (`pageable`, `sort.unsorted`,
 * `numberOfElements`) que engessam a API a uma versao do Spring e confundem
 * quem consome. Aqui expomos so o que o cliente precisa.
 */
@Schema(description = "Pagina de resultados")
public record PageResponse<T>(

        List<T> conteudo,

        @Schema(description = "Pagina atual, comecando em 0", example = "0")
        int pagina,

        @Schema(description = "Itens por pagina", example = "20")
        int tamanho,

        @Schema(description = "Total de itens", example = "137")
        long totalItens,

        @Schema(description = "Total de paginas", example = "7")
        int totalPaginas,

        @Schema(description = "Indica se esta e a ultima pagina")
        boolean ultima
) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> conversor) {
        return new PageResponse<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return from(page, Function.identity());
    }
}
