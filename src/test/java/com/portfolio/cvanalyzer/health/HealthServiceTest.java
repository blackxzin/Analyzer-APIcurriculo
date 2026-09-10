package com.portfolio.cvanalyzer.health;

import com.portfolio.cvanalyzer.health.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste UNITARIO: sem Spring, sem banco, sem rede. Milissegundos para rodar.
 *
 * So e possivel porque HealthService recebe tudo por construtor.
 * Esse e o ganho pratico da injecao por construtor.
 */
class HealthServiceTest {

    private static final Instant MOMENTO_FIXO = Instant.parse("2026-01-01T12:00:00Z");

    @Test
    @DisplayName("retorna status UP com nome e versao da aplicacao")
    void retornaStatusUp() {
        // Arrange
        Clock relogioFixo = Clock.fixed(MOMENTO_FIXO, ZoneOffset.UTC);
        HealthService service = new HealthService(relogioFixo, "cv-analyzer-api", "1.2.3");

        // Act
        HealthResponse resposta = service.check();

        // Assert
        assertThat(resposta.status()).isEqualTo("UP");
        assertThat(resposta.application()).isEqualTo("cv-analyzer-api");
        assertThat(resposta.version()).isEqualTo("1.2.3");
        assertThat(resposta.checkedAt()).isEqualTo(MOMENTO_FIXO);
    }
}
