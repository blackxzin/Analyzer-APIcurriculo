package com.portfolio.cvanalyzer.report.exception;

import com.portfolio.cvanalyzer.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Falha ao montar o PDF do relatorio.
 *
 * Vira 500 porque o cliente nao fez nada de errado: a analise existe, quem
 * falhou fomos nos. A causa original vai para o log, nunca para a resposta.
 */
public class ReportGenerationException extends BusinessException {

    public ReportGenerationException(Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR,
                "FALHA_AO_GERAR_RELATORIO",
                "Nao foi possivel gerar o relatorio em PDF.");
        initCause(cause);
    }
}
