package com.portfolio.cvanalyzer.auth.exception;

import com.portfolio.cvanalyzer.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * E-mail ja cadastrado. Vira HTTP 409 (conflito).
 */
public class EmailAlreadyUsedException extends BusinessException {

    public EmailAlreadyUsedException(String email) {
        super(HttpStatus.CONFLICT,
                "EMAIL_JA_CADASTRADO",
                "Ja existe um usuario cadastrado com o e-mail: %s".formatted(email));
    }
}
