package com.portfolio.cvanalyzer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.portfolio.cvanalyzer.support.IntegrationTest;
import com.portfolio.cvanalyzer.support.PdfTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de ponta a ponta do fluxo principal do produto:
 * enviar curriculo -> cadastrar vaga -> analisar -> consultar historico.
 *
 * Este e o teste que mais protege o projeto. Os unitarios garantem que cada
 * peca funciona; este garante que elas funcionam JUNTAS, com banco real,
 * JSON real e HTTP real.
 */
@IntegrationTest
/**
 * Roda como ADMIN: as regras de acesso ja tem teste proprio em
 * AuthIntegrationTest, e repetir cadastro e login em cada teste de fluxo
 * so tornaria o teste mais longo sem cobrir nada de novo.
 */
@WithMockUser(roles = "ADMIN")
class CvAnalyzerFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String VAGA_JSON = """
            {
              "titulo": "Desenvolvedor Java Backend",
              "empresa": "Acme Tecnologia",
              "senioridade": "PLENO",
              "descricao": "Vaga para desenvolvedor backend em time de produto.",
              "requisitosObrigatorios": ["Java", "SQL", "springboot", "Docker"],
              "requisitosDesejaveis": ["Git"]
            }
            """;

    @Test
    @DisplayName("fluxo completo: upload, vaga, analise e historico")
    void fluxoCompleto() throws Exception {
        // ---------- 1. Upload do curriculo ----------
        MockMultipartFile arquivo = new MockMultipartFile(
                "file", "curriculo-maria.pdf", MediaType.APPLICATION_PDF_VALUE,
                PdfTestFactory.curriculoExemplo());

        String respostaUpload = mockMvc.perform(multipart("/api/v1/resumes").file(arquivo))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nome").value("Maria Souza Lima"))
                .andExpect(jsonPath("$.data.email").value("maria.souza@email.com"))
                .andExpect(jsonPath("$.data.telefone").value("(11) 98765-4321"))
                .andExpect(jsonPath("$.data.arquivo").value("curriculo-maria.pdf"))
                .andExpect(jsonPath("$.data.paginas").value(1))
                .andExpect(jsonPath("$.data.experiencias").isNotEmpty())
                .andExpect(jsonPath("$.data.formacao").isNotEmpty())
                .andExpect(jsonPath("$.data.idiomas").isNotEmpty())
                .andExpect(jsonPath("$.data.criadoEm").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String curriculoId = json(respostaUpload).path("data").path("id").asText();
        JsonNode tecnologias = json(respostaUpload).path("data").path("tecnologias");
        assertThat(tecnologias.toString()).contains("Java").contains("SQL").contains("Git");

        // ---------- 2. Cadastro da vaga ----------
        String respostaVaga = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VAGA_JSON))
                .andExpect(status().isCreated())
                // "springboot" foi canonicalizado para "Spring Boot":
                // sem isso a comparacao com o curriculo seria injusta.
                .andExpect(jsonPath("$.data.requisitosObrigatorios",
                        org.hamcrest.Matchers.hasItem("Spring Boot")))
                .andExpect(jsonPath("$.data.requisitosDesejaveis[0]").value("Git"))
                .andExpect(jsonPath("$.data.criadoEm").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String vagaId = json(respostaVaga).path("data").path("id").asText();

        // ---------- 3. Analise ----------
        String corpoAnalise = objectMapper.writeValueAsString(
                java.util.Map.of("curriculoId", curriculoId, "vagaId", vagaId));

        String respostaAnalise = mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAnalise))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.candidato").value("Maria Souza Lima"))
                .andExpect(jsonPath("$.data.vaga").value("Desenvolvedor Java Backend"))
                .andExpect(jsonPath("$.data.totalRequisitos").value(5))
                .andExpect(jsonPath("$.data.pontosFortes",
                        org.hamcrest.Matchers.hasItems("Java", "SQL", "Git")))
                .andExpect(jsonPath("$.data.requisitosAusentes",
                        org.hamcrest.Matchers.hasItems("Spring Boot", "Docker")))
                .andExpect(jsonPath("$.data.recomendacoes").isNotEmpty())
                // Regressao: o campo ja saiu null por falta de flush.
                .andExpect(jsonPath("$.data.analisadoEm").isNotEmpty())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode analise = json(respostaAnalise).path("data");
        int compatibilidade = analise.path("compatibilidade").asInt();

        // Obrigatorios: Java e SQL atendidos, Spring Boot e Docker ausentes
        //               -> 6 de 12. Desejavel Git atendido -> 1 de 1.
        // Nota: 7/13 = 53,8% -> 54%.
        assertThat(compatibilidade).isEqualTo(54);

        String analiseId = analise.path("id").asText();

        // ---------- 4. Busca da analise gravada ----------
        mockMvc.perform(get("/api/v1/analyses/{id}", analiseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.compatibilidade").value(compatibilidade));

        // ---------- 5. Historico filtrado por curriculo ----------
        mockMvc.perform(get("/api/v1/analyses").param("curriculoId", curriculoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conteudo[0].id").value(analiseId))
                .andExpect(jsonPath("$.data.conteudo[0].empresa").value("Acme Tecnologia"))
                .andExpect(jsonPath("$.data.totalItens").value(1));

        // Filtro por outra vaga inexistente devolve pagina vazia, nao erro.
        mockMvc.perform(get("/api/v1/analyses")
                        .param("vagaId", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conteudo").isEmpty());

        // ---------- 6. Relatorio em PDF ----------
        byte[] pdf = mockMvc.perform(get("/api/v1/analyses/{id}/relatorio", analiseId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("analise-%s.pdf".formatted(analiseId))))
                .andReturn().getResponse().getContentAsByteArray();

        // %PDF nos primeiros bytes: o que voltou e mesmo um PDF, nao um JSON de erro.
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        // ---------- 7. Remocao em cascata ----------
        mockMvc.perform(delete("/api/v1/resumes/{id}", curriculoId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/{id}", curriculoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECURSO_NAO_ENCONTRADO"));

        // A analise foi apagada junto com o curriculo (ON DELETE CASCADE).
        mockMvc.perform(get("/api/v1/analyses/{id}", analiseId))
                .andExpect(status().isNotFound());
    }

    private JsonNode json(String conteudo) throws Exception {
        return objectMapper.readTree(conteudo);
    }
}
