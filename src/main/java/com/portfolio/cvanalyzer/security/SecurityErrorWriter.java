package com.portfolio.cvanalyzer.security;

import com.portfolio.cvanalyzer.common.dto.ApiError;
import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve erros de seguranca no MESMO envelope do resto da API.
 *
 * Sem isto o cliente receberia dois formatos de erro diferentes: o nosso
 * ApiResponse para erros de negocio e a pagina/JSON padrao do Spring
 * Security para 401 e 403 — justamente os erros que todo cliente precisa
 * tratar. O motivo de existir a classe: os handlers de 401 e 403 rodam
 * ANTES do @RestControllerAdvice, que so enxerga o que chega ao controller.
 */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> corpo = ApiResponse.fail(ApiError.of(code, message));
        response.getWriter().write(objectMapper.writeValueAsString(corpo));
    }
}
