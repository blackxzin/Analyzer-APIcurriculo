package com.portfolio.cvanalyzer.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Resposta para usuario autenticado que nao tem o papel exigido: HTTP 403.
 *
 * 401 e 403 dizem coisas diferentes e trocar um pelo outro confunde o
 * cliente: 401 e "nao sei quem voce e, faca login"; 403 e "sei quem voce e,
 * e voce nao pode".
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    public ApiAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        errorWriter.write(response,
                HttpStatus.FORBIDDEN,
                "ACESSO_NEGADO",
                "Seu usuario nao tem permissao para esta operacao.");
    }
}
