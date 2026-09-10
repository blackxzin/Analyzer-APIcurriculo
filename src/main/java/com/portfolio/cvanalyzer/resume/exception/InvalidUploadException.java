package com.portfolio.cvanalyzer.resume.exception;

import com.portfolio.cvanalyzer.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * O upload em si esta errado: arquivo ausente, tipo nao permitido,
 * tamanho acima do limite. Erro do cliente -> 400.
 */
public class InvalidUploadException extends BusinessException {

    public InvalidUploadException(String message) {
        super(HttpStatus.BAD_REQUEST, "UPLOAD_INVALIDO", message);
    }
}
