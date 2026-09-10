package com.portfolio.cvanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Beans de infraestrutura da aplicacao.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Relogio do sistema como bean.
     *
     * Motivo: chamar Instant.now() espalhado pelo codigo torna impossivel
     * testar comportamento dependente de tempo. Com um Clock injetado,
     * o teste troca por Clock.fixed(...) e o resultado vira deterministico.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
