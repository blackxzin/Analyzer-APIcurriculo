package com.portfolio.cvanalyzer;

import com.portfolio.cvanalyzer.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste de fumaca: sobe a aplicacao inteira contra um PostgreSQL real,
 * criado em container.
 *
 * Por que nao H2 em memoria? Porque H2 nao e PostgreSQL: aceita SQL que o
 * Postgres recusa e vice-versa. Testar no mesmo banco de producao elimina a
 * classe de bug "passou no teste, quebrou em producao". As migrations Flyway
 * tambem rodam aqui, entao qualquer divergencia entre schema e entidade
 * derruba este teste.
 *
 * Requer Docker rodando na maquina.
 */
@IntegrationTest
class CvAnalyzerApiApplicationTests {

    @Test
    @DisplayName("o contexto carrega e o schema bate com as entidades")
    void contextLoads() {
        // Falha na subida se qualquer bean ou mapeamento estiver errado.
    }
}
