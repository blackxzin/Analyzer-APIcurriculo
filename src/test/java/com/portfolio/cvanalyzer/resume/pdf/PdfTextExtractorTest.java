package com.portfolio.cvanalyzer.resume.pdf;

import com.portfolio.cvanalyzer.config.ResumeProperties;
import com.portfolio.cvanalyzer.resume.exception.InvalidPdfException;
import com.portfolio.cvanalyzer.support.PdfTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractorTest {

    private final ResumeProperties properties = new ResumeProperties(
            10 * 1024 * 1024, 20, 50, List.of("application/pdf"));

    private final PdfTextExtractor extractor = new PdfTextExtractor(properties);

    @Test
    @DisplayName("extrai o texto de um PDF valido")
    void extraiTexto() {
        ExtractedPdf resultado = extractor.extract(PdfTestFactory.curriculoExemplo());

        assertThat(resultado.pageCount()).isEqualTo(1);
        assertThat(resultado.text())
                .contains("Maria Souza Lima")
                .contains("maria.souza@email.com")
                .contains("EXPERIENCIA PROFISSIONAL");
    }

    @Test
    @DisplayName("rejeita arquivo que nao comeca com a assinatura de PDF")
    void rejeitaNaoPdf() {
        byte[] textoPuro = "isto nao e um pdf, e apenas texto".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract(textoPuro))
                .isInstanceOf(InvalidPdfException.class)
                .hasMessageContaining("nao e um PDF");
    }

    @Test
    @DisplayName("rejeita PDF corrompido (assinatura certa, conteudo quebrado)")
    void rejeitaPdfCorrompido() {
        byte[] corrompido = "%PDF-1.7 conteudo destruido".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract(corrompido))
                .isInstanceOf(InvalidPdfException.class)
                .hasMessageContaining("corrompido");
    }

    @Test
    @DisplayName("rejeita arquivo vazio")
    void rejeitaVazio() {
        assertThatThrownBy(() -> extractor.extract(new byte[0]))
                .isInstanceOf(InvalidPdfException.class);
        assertThatThrownBy(() -> extractor.extract(null))
                .isInstanceOf(InvalidPdfException.class);
    }

    @Test
    @DisplayName("rejeita PDF com texto curto demais para ser um curriculo")
    void rejeitaTextoInsuficiente() {
        byte[] quaseVazio = PdfTestFactory.pdfComTexto(List.of("oi"));

        assertThatThrownBy(() -> extractor.extract(quaseVazio))
                .isInstanceOf(InvalidPdfException.class)
                .hasMessageContaining("escaneados");
    }

    @Test
    @DisplayName("rejeita PDF com mais paginas que o limite")
    void rejeitaExcessoDePaginas() {
        ResumeProperties limiteBaixo = new ResumeProperties(
                10 * 1024 * 1024, 1, 10, List.of("application/pdf"));
        PdfTextExtractor comLimite = new PdfTextExtractor(limiteBaixo);

        byte[] tresPaginas = PdfTestFactory.pdfComPaginas(3,
                "Curriculo de teste com texto suficiente para passar da validacao minima.");

        assertThatThrownBy(() -> comLimite.extract(tresPaginas))
                .isInstanceOf(InvalidPdfException.class)
                .hasMessageContaining("o limite e 1");
    }
}
