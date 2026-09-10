package com.portfolio.cvanalyzer.health;

import com.portfolio.cvanalyzer.health.dto.HealthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste da CAMADA WEB apenas.
 *
 * Sobe so o necessario para servir HTTP (controllers, conversores JSON,
 * exception handler). Nao sobe banco nem services reais — o service e
 * substituido por um mock. Assim o teste falha por problema de controller,
 * nunca por banco fora do ar.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @Test
    @DisplayName("GET /api/v1/health devolve 200 com envelope de sucesso")
    void deveRetornarStatusUp() throws Exception {
        // Arrange
        given(healthService.check()).willReturn(new HealthResponse(
                "UP", "cv-analyzer-api", "0.0.1-SNAPSHOT", Instant.parse("2026-01-01T12:00:00Z")));

        // Act + Assert
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.application").value("cv-analyzer-api"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("rota inexistente devolve 404 no envelope de erro, nao 500")
    void rotaInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(get("/api/v1/rota-que-nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROTA_NAO_ENCONTRADA"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("metodo HTTP errado devolve 405")
    void metodoErradoDeveRetornar405() throws Exception {
        mockMvc.perform(post("/api/v1/health"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error.code").value("METODO_NAO_PERMITIDO"));
    }
}
