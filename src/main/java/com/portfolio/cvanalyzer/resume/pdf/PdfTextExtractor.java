package com.portfolio.cvanalyzer.resume.pdf;

import com.portfolio.cvanalyzer.config.ResumeProperties;
import com.portfolio.cvanalyzer.resume.exception.InvalidPdfException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extrai o texto de um PDF usando Apache PDFBox.
 *
 * Classe com UMA responsabilidade: bytes de PDF -> texto. Nao sabe o que e
 * um curriculo, nao fala com banco. Isso a torna trivial de testar e
 * reaproveitavel se amanha precisarmos ler outro tipo de documento.
 */
@Component
public class PdfTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    /** Todo PDF valido comeca com estes 5 bytes. */
    private static final byte[] ASSINATURA_PDF = {'%', 'P', 'D', 'F', '-'};

    private final ResumeProperties properties;

    public PdfTextExtractor(ResumeProperties properties) {
        this.properties = properties;
    }

    public ExtractedPdf extract(byte[] conteudo) {
        validarAssinatura(conteudo);

        // try-with-resources: o PDDocument segura memoria nativa e PRECISA
        // ser fechado. Sem isso, cada upload vaza memoria ate derrubar a JVM.
        try (PDDocument documento = Loader.loadPDF(conteudo)) {

            if (documento.isEncrypted()) {
                throw new InvalidPdfException(
                        "O PDF esta protegido por senha e nao pode ser lido.");
            }

            int paginas = documento.getNumberOfPages();
            if (paginas == 0) {
                throw new InvalidPdfException("O PDF nao possui paginas.");
            }
            if (paginas > properties.maxPages()) {
                throw new InvalidPdfException(
                        "O PDF possui %d paginas; o limite e %d."
                                .formatted(paginas, properties.maxPages()));
            }

            PDFTextStripper stripper = new PDFTextStripper();
            // Sem isto, PDF em duas colunas sai com as colunas intercaladas
            // e o texto vira sopa de letrinhas.
            stripper.setSortByPosition(true);
            String texto = stripper.getText(documento);

            if (texto == null || texto.strip().length() < properties.minTextLength()) {
                throw new InvalidPdfException(
                        "Nao foi possivel extrair texto do PDF. "
                                + "Curriculos escaneados como imagem nao sao suportados.");
            }

            return new ExtractedPdf(texto, paginas);

        } catch (IOException e) {
            // Mensagem do PDFBox pode expor detalhe interno: fica so no log.
            log.warn("Falha ao ler PDF: {}", e.getMessage());
            throw new InvalidPdfException("O arquivo enviado nao e um PDF valido ou esta corrompido.");
        }
    }

    private void validarAssinatura(byte[] conteudo) {
        if (conteudo == null || conteudo.length < ASSINATURA_PDF.length) {
            throw new InvalidPdfException("O arquivo enviado esta vazio ou e pequeno demais.");
        }
        for (int i = 0; i < ASSINATURA_PDF.length; i++) {
            if (conteudo[i] != ASSINATURA_PDF[i]) {
                // Checar os bytes, e nao so a extensao ou o Content-Type:
                // ambos vem do cliente e podem mentir.
                throw new InvalidPdfException("O arquivo enviado nao e um PDF.");
            }
        }
    }
}
