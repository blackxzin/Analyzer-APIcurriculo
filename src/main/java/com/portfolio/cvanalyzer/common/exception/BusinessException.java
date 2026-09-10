package com.portfolio.cvanalyzer.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excecao base de regra de negocio.
 *
 * Estende RuntimeException (unchecked) de proposito: em Spring, excecao checada
 * obrigaria try/catch em toda a cadeia e ainda faria o rollback da transacao
 * NAO acontecer por padrao. Unchecked mantem o codigo limpo e o rollback correto.
 *
 * Carrega o HttpStatus junto para o handler global saber qual codigo devolver
 * sem precisar de uma cadeia de `if (e instanceof ...)`.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
