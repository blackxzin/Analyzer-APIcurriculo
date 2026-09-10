package com.portfolio.cvanalyzer.report;

import com.portfolio.cvanalyzer.analysis.dto.AnalysisResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O gerador e testado lendo de volta o PDF que ele acabou de escrever.
 *
 * Afirmar so "gerou algum byte" nao prova nada — um PDF em branco tambem
 * gera bytes. Extrair o texto e conferir o conteudo prova que a informacao
 * chegou na pagina.
 */
class AnalysisReportGeneratorTest {

    private final AnalysisReportGenerator generator = new AnalysisReportGenerator();

    private AnalysisResponse analise(List<String> fortes,
                                     List<String> ausentes,
                                     List<String> recomendacoes) {
        return new AnalysisResponse(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Ana Souza",
                "Desenvolvedor Java Backend",
                78,
                fortes,
                ausentes,
                recomendacoes,
                9,
                Instant.parse("2026-01-15T10:00:00Z"));
    }

    private String textoDo(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    @Test
    @DisplayName("gera um PDF valido com os dados da analise")
    void geraPdfComOsDados() throws IOException {
        // Arrange
        AnalysisResponse analise = analise(
                List.of("Java", "SQL"),
                List.of("Docker", "Kubernetes"),
                List.of("Estudar Docker para conteinerizar aplicacoes"));

        // Act
        byte[] pdf = generator.generate(analise);

        // Assert
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");

        String texto = textoDo(pdf);
        assertThat(texto)
                .contains("Relatorio de Analise de Curriculo")
                .contains("Ana Souza")
                .contains("Desenvolvedor Java Backend")
                .contains("78%")
                .contains("Pontos fortes")
                .contains("Java")
                .contains("Requisitos ausentes")
                .contains("Kubernetes")
                .contains("Recomendacoes")
                .contains("Estudar Docker");
    }

    @Test
    @DisplayName("secao vazia vira frase explicativa, e nao um buraco no relatorio")
    void secaoVaziaGanhaTexto() throws IOException {
        byte[] pdf = generator.generate(analise(List.of(), List.of(), List.of()));

        assertThat(textoDo(pdf))
                .contains("Nenhum requisito da vaga foi encontrado no curriculo.")
                .contains("Nenhum. O curriculo atende a todos os requisitos.")
                .contains("Sem recomendacoes para esta analise.");
    }

    @Test
    @DisplayName("texto com acento e simbolo fora da tabela WinAnsi nao quebra a geracao")
    void normalizaCaracteresForaDaTabela() throws IOException {
        AnalysisResponse analise = analise(
                List.of("Java", "Programacao Orientada a Objetos"),
                List.of("Kubernetes"),
                List.of("Estudar padrões de projeto 你好 e praticar com um projeto real"));

        String texto = textoDo(generator.generate(analise));

        assertThat(texto).contains("Estudar padroes de projeto");
    }

    @Test
    @DisplayName("conteudo longo transborda para novas paginas")
    void quebraEmVariasPaginas() throws IOException {
        List<String> muitasRecomendacoes = IntStream.rangeClosed(1, 90)
                .mapToObj(i -> "Recomendacao numero %d: estudar o assunto, praticar em um projeto e revisar."
                        .formatted(i))
                .toList();

        byte[] pdf = generator.generate(analise(List.of("Java"), List.of("Docker"), muitasRecomendacoes));

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertThat(documento.getNumberOfPages()).isGreaterThan(1);
        }
        assertThat(textoDo(pdf)).contains("Recomendacao numero 90");
    }
}
