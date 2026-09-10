package com.portfolio.cvanalyzer.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Envelope padrao de TODA resposta da API.
 *
 * Por que envelope?
 *  - o cliente sempre recebe a mesma forma, sucesso ou erro;
 *  - da espaco para metadados (paginacao, timestamp) sem quebrar o contrato.
 *
 * E um `record`: imutavel por natureza (campos final, sem setters).
 * Isso atende a regra de imutabilidade — nunca alteramos, sempre criamos novo.
 *
 * @JsonInclude(NON_NULL) esconde campos nulos no JSON, entao um sucesso
 * nao carrega "error": null inutilmente.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ApiError error,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error, Instant.now());
    }
}
