package com.portfolio.cvanalyzer.resume.exception;

import com.portfolio.cvanalyzer.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Arquivo enviado nao e um PDF utilizavel: corrompido, protegido por senha,
 * vazio ou sem texto extraivel (PDF que e so imagem escaneada).
 *
 * 422 UNPROCESSABLE_ENTITY e nao 400: a requisicao esta bem formada,
 * o conteudo dela e que nao da para processar.
 */
public class InvalidPdfException extends BusinessException {

    public InvalidPdfException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "PDF_INVALIDO", message);
    }
}
