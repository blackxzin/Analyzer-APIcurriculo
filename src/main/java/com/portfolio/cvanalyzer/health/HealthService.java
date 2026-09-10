package com.portfolio.cvanalyzer.health;

import com.portfolio.cvanalyzer.health.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Camada de servico: onde mora a regra.
 *
 * Aqui a regra e trivial, mas o endpoint ja nasce na estrutura certa —
 * o controller nunca monta a resposta sozinho.
 *
 * Injecao por CONSTRUTOR (nao @Autowired em campo) porque:
 *  - as dependencias ficam `final`, logo imutaveis;
 *  - da para instanciar a classe num teste unitario sem subir o Spring;
 *  - dependencia faltando quebra na compilacao, nao em producao.
 *
 * Clock injetado em vez de Instant.now() fixo: torna o tempo testavel.
 */
@Service
public class HealthService {

    private final Clock clock;
    private final String applicationName;
    private final String applicationVersion;

    public HealthService(Clock clock,
                         @Value("${spring.application.name}") String applicationName,
                         @Value("${app.version}") String applicationVersion) {
        this.clock = clock;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    public HealthResponse check() {
        return new HealthResponse("UP", applicationName, applicationVersion, Instant.now(clock));
    }
}
