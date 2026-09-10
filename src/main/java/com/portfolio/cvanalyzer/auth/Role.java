package com.portfolio.cvanalyzer.auth;

/**
 * Papel do usuario dentro da API.
 *
 * Dois papeis bastam para este dominio, e cada um existe por um motivo:
 *  RECRUTADOR: usa a API no dia a dia — envia curriculo, cadastra vaga, analisa.
 *  ADMIN     : tudo isso mais as operacoes destrutivas (remover curriculo ou
 *              vaga, o que apaga o historico de analises junto).
 *
 * Guardamos o nome puro no banco (`RECRUTADOR`) e o Spring Security recebe
 * a authority com o prefixo `ROLE_`, que e a convencao que `hasRole(...)`
 * espera. Deixar o prefixo dentro do enum vazaria detalhe de framework
 * para dentro do dominio e para o banco.
 */
public enum Role {

    RECRUTADOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
