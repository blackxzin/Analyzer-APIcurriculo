package com.portfolio.cvanalyzer.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Detalhe do erro devolvido dentro de ApiResponse.
 *
 * `code`    : identificador estavel, o cliente pode reagir a ele (RECURSO_NAO_ENCONTRADO)
 * `message` : texto legivel para humano
 * `details` : lista opcional, usada em erro de validacao (campo a campo)
 *
 * Nunca colocamos stack trace aqui — vazaria estrutura interna do sistema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        List<FieldError> details
) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public record FieldError(String field, String message) {
    }
}
