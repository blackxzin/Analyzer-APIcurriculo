package com.portfolio.cvanalyzer.auth.exception;

import com.portfolio.cvanalyzer.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Login invalido. Vira HTTP 401.
 *
 * A mensagem e deliberadamente vaga — "credenciais invalidas", nunca
 * "esse e-mail nao existe". Distinguir os dois casos entrega ao atacante
 * uma lista de e-mails validos do sistema (user enumeration).
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED,
                "CREDENCIAIS_INVALIDAS",
                "E-mail ou senha invalidos.");
    }
}
