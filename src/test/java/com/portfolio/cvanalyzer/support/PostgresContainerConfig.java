package com.portfolio.cvanalyzer.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Um unico container PostgreSQL para TODOS os testes de integracao.
 *
 * O container e estatico e sobe no bloco `static`: assim ele nasce uma vez
 * por JVM e todas as classes de teste compartilham. Se cada classe subisse
 * o proprio banco, a suite passaria de segundos para minutos.
 *
 * @ServiceConnection faz o Spring configurar o DataSource (e portanto o
 * Flyway e o Hibernate) apontando para este container, sem uma linha de URL
 * escrita na mao.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return POSTGRES;
    }
}
