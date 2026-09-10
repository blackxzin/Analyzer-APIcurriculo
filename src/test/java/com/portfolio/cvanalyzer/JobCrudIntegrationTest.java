package com.portfolio.cvanalyzer;

import com.portfolio.cvanalyzer.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ciclo de vida completo de uma vaga: criar, ler, atualizar, listar, remover.
 */
@IntegrationTest
/**
 * Roda como ADMIN: as regras de acesso ja tem teste proprio em
 * AuthIntegrationTest, e repetir cadastro e login em cada teste de fluxo
 * so tornaria o teste mais longo sem cobrir nada de novo.
 */
@WithMockUser(roles = "ADMIN")
class JobCrudIntegrationTest {

    private static final Pattern ID_NO_JSON = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    private String criarVaga(String json) throws Exception {
        String resposta = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Matcher matcher = ID_NO_JSON.matcher(resposta);
        assertThat(matcher.find()).as("resposta deve conter o id da vaga").isTrue();
        return matcher.group(1);
    }

    @Test
    @DisplayName("cria, busca, atualiza, lista e remove uma vaga")
    void cicloDeVida() throws Exception {
        String id = criarVaga("""
                {
                  "titulo": "Dev Java Junior",
                  "empresa": "Acme",
                  "senioridade": "JUNIOR",
                  "descricao": "Vaga inicial para quem esta comecando na carreira backend.",
                  "requisitosObrigatorios": ["Java", "Git"],
                  "requisitosDesejaveis": ["Docker"]
                }
                """);

        mockMvc.perform(get("/api/v1/jobs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titulo").value("Dev Java Junior"))
                .andExpect(jsonPath("$.data.senioridade").value("JUNIOR"))
                .andExpect(jsonPath("$.data.requisitosObrigatorios.length()").value(2))
                .andExpect(jsonPath("$.data.requisitosDesejaveis[0]").value("Docker"));

        mockMvc.perform(put("/api/v1/jobs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Dev Java Pleno",
                                  "empresa": "Acme Tecnologia",
                                  "senioridade": "PLENO",
                                  "descricao": "Vaga atualizada para pessoa desenvolvedora pleno.",
                                  "requisitosObrigatorios": ["Java", "Spring Boot", "PostgreSQL"],
                                  "requisitosDesejaveis": ["Kubernetes"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.titulo").value("Dev Java Pleno"))
                .andExpect(jsonPath("$.data.senioridade").value("PLENO"))
                .andExpect(jsonPath("$.data.requisitosObrigatorios.length()").value(3))
                .andExpect(jsonPath("$.data.requisitosObrigatorios",
                        org.hamcrest.Matchers.hasItem("Spring Boot")));

        mockMvc.perform(get("/api/v1/jobs").param("tamanho", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conteudo[*].id",
                        org.hamcrest.Matchers.hasItem(id)))
                .andExpect(jsonPath("$.data.conteudo[*].totalRequisitos",
                        org.hamcrest.Matchers.hasItem(4)));

        mockMvc.perform(delete("/api/v1/jobs/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/jobs/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.message",
                        org.hamcrest.Matchers.containsString("Vaga")));
    }

    @Test
    @DisplayName("requisito repetido nas duas listas fica apenas como obrigatorio")
    void requisitoDuplicado() throws Exception {
        String id = criarVaga("""
                {
                  "titulo": "Dev Backend",
                  "empresa": "Acme",
                  "senioridade": "PLENO",
                  "descricao": "Vaga backend com foco em Java e ecossistema Spring.",
                  "requisitosObrigatorios": ["Java", "java", "JDK"],
                  "requisitosDesejaveis": ["Java", "Docker"]
                }
                """);

        // "Java", "java" e "JDK" sao o mesmo requisito canonico: sobra um so.
        mockMvc.perform(get("/api/v1/jobs/{id}", id))
                .andExpect(jsonPath("$.data.requisitosObrigatorios.length()").value(1))
                .andExpect(jsonPath("$.data.requisitosObrigatorios[0]").value("Java"))
                .andExpect(jsonPath("$.data.requisitosDesejaveis.length()").value(1))
                .andExpect(jsonPath("$.data.requisitosDesejaveis[0]").value("Docker"));

        mockMvc.perform(delete("/api/v1/jobs/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("atualizar vaga inexistente devolve 404")
    void atualizarInexistente() throws Exception {
        mockMvc.perform(put("/api/v1/jobs/{id}", "22222222-2222-2222-2222-222222222222")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Qualquer",
                                  "empresa": "Acme",
                                  "senioridade": "PLENO",
                                  "descricao": "Descricao suficientemente longa com Java.",
                                  "requisitosObrigatorios": ["Java"]
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("remover vaga inexistente devolve 404")
    void removerInexistente() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/{id}", "33333333-3333-3333-3333-333333333333"))
                .andExpect(status().isNotFound());
    }
}
