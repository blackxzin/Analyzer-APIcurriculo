package com.portfolio.cvanalyzer.job.exception;

import com.portfolio.cvanalyzer.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Vaga sem nenhum requisito nao pode existir: nao ha o que comparar,
 * e qualquer nota de compatibilidade seria inventada.
 *
 * Falhar aqui e melhor que gravar uma vaga que produz analise sem sentido.
 */
public class JobWithoutRequirementsException extends BusinessException {

    public JobWithoutRequirementsException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "VAGA_SEM_REQUISITOS",
                "Nao foi possivel identificar requisitos na descricao. "
                        + "Informe ao menos um requisito obrigatorio.");
    }
}
