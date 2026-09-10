package com.portfolio.cvanalyzer.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Lancada quando um recurso pedido por id nao existe. Vira HTTP 404.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND,
                "RECURSO_NAO_ENCONTRADO",
                "%s nao encontrado para o identificador: %s".formatted(resource, id));
    }
}
