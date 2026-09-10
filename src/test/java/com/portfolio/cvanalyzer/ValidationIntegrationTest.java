package com.portfolio.cvanalyzer;

import com.portfolio.cvanalyzer.support.IntegrationTest;
import com.portfolio.cvanalyzer.support.PdfTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caminhos de erro.
 *
 * Testar so o caminho feliz da uma falsa sensacao de seguranca: em producao
 * o que mais chega e requisicao errada. Aqui garantimos que cada erro vira
 * o status HTTP certo e o envelope certo — nunca um 500 generico.
 */
@IntegrationTest
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String UUID_INEXISTENTE = "11111111-1111-1111-1111-111111111111";

    @Test
    @DisplayName("upload de arquivo que nao e PDF devolve 422")
    void uploadNaoPdf() throws Exception {
        MockMultipartFile falso = new MockMultipartFile(
                "file", "curriculo.pdf", MediaType.APPLICATION_PDF_VALUE,
                "isto e texto puro, nao um pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/resumes").file(falso))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PDF_INVALIDO"));
    }

    @Test
    @DisplayName("upload com Content-Type nao permitido devolve 400")
    void uploadTipoInvalido() throws Exception {
        MockMultipartFile docx = new MockMultipartFile(
                "file", "curriculo.docx", "application/msword",
                PdfTestFactory.curriculoExemplo());

        mockMvc.perform(multipart("/api/v1/resumes").file(docx))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UPLOAD_INVALIDO"));
    }

    @Test
    @DisplayName("upload de arquivo vazio devolve 400")
    void uploadVazio() throws Exception {
        MockMultipartFile vazio = new MockMultipartFile(
                "file", "vazio.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/v1/resumes").file(vazio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("UPLOAD_INVALIDO"));
    }

    @Test
    @DisplayName("curriculo inexistente devolve 404 no envelope de erro")
    void curriculoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/{id}", UUID_INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.error.message").value(
                        org.hamcrest.Matchers.containsString("Curriculo")));
    }

    @Test
    @DisplayName("id em formato invalido devolve 400, e nao 500")
    void idMalFormado() throws Exception {
        mockMvc.perform(get("/api/v1/resumes/{id}", "nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PARAMETRO_INVALIDO"));
    }

    @Test
    @DisplayName("vaga com campos obrigatorios faltando devolve 400 campo a campo")
    void vagaInvalida() throws Exception {
        String corpo = """
                {
                  "titulo": "",
                  "empresa": "",
                  "descricao": "curta"
                }
                """;

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ERRO_DE_VALIDACAO"))
                .andExpect(jsonPath("$.error.details").isArray())
                .andExpect(jsonPath("$.error.details[*].field",
                        org.hamcrest.Matchers.hasItems("titulo", "empresa", "senioridade", "descricao")));
    }

    @Test
    @DisplayName("JSON malformado devolve 400 com codigo proprio")
    void jsonMalformado() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto nao e json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CORPO_INVALIDO"));
    }

    @Test
    @DisplayName("vaga sem requisito identificavel na descricao devolve 422")
    void vagaSemRequisitos() throws Exception {
        String corpo = """
                {
                  "titulo": "Analista",
                  "empresa": "Acme",
                  "senioridade": "JUNIOR",
                  "descricao": "Vaga para pessoa dedicada, comunicativa e com vontade de aprender."
                }
                """;

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("VAGA_SEM_REQUISITOS"));
    }

    @Test
    @DisplayName("vaga sem requisitos informados extrai da descricao automaticamente")
    void vagaExtraiRequisitosDaDescricao() throws Exception {
        String corpo = """
                {
                  "titulo": "Pessoa Desenvolvedora Backend",
                  "empresa": "Acme",
                  "senioridade": "SENIOR",
                  "descricao": "Procuramos alguem com Java, Spring Boot, Docker e PostgreSQL."
                }
                """;

        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requisitosObrigatorios",
                        org.hamcrest.Matchers.hasItems(
                                "Java", "Spring Boot", "Docker", "PostgreSQL")))
                .andExpect(jsonPath("$.data.requisitosDesejaveis").isEmpty());
    }

    @Test
    @DisplayName("analise com curriculo inexistente devolve 404")
    void analiseComCurriculoInexistente() throws Exception {
        String corpo = """
                { "curriculoId": "%s", "vagaId": "%s" }
                """.formatted(UUID_INEXISTENTE, UUID_INEXISTENTE);

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("analise sem os ids obrigatorios devolve 400")
    void analiseSemIds() throws Exception {
        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[*].field",
                        org.hamcrest.Matchers.hasItems("curriculoId", "vagaId")));
    }

    @Test
    @DisplayName("listagem limita o tamanho de pagina pedido")
    void listagemLimitaTamanho() throws Exception {
        mockMvc.perform(get("/api/v1/resumes")
                        .param("tamanho", "999999")
                        .param("pagina", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tamanho").value(100))
                .andExpect(jsonPath("$.data.pagina").value(0));
    }

    @Test
    @DisplayName("nome de arquivo com path traversal e sanitizado")
    void sanitizaNomeDeArquivo() throws Exception {
        MockMultipartFile malicioso = new MockMultipartFile(
                "file", "../../../etc/passwd.pdf", MediaType.APPLICATION_PDF_VALUE,
                PdfTestFactory.pdfComTexto(List.of(
                        "Joao da Silva",
                        "joao@email.com",
                        "Experiencia com Java e Docker em projetos de backend corporativo.")));

        mockMvc.perform(multipart("/api/v1/resumes").file(malicioso))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.arquivo").value("passwd.pdf"));
    }
}
