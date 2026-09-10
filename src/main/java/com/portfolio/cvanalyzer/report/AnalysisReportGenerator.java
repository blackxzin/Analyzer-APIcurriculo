package com.portfolio.cvanalyzer.report;

import com.portfolio.cvanalyzer.analysis.dto.AnalysisResponse;
import com.portfolio.cvanalyzer.report.exception.ReportGenerationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Monta o relatorio da analise em PDF.
 *
 * Usamos as fontes padrao do PDF (Helvetica) e nao um arquivo .ttf embutido:
 * elas existem em todo leitor de PDF, o arquivo final fica com poucos KB e
 * o projeto nao ganha um binario de fonte no repositorio.
 *
 * O PDF e gerado em memoria e devolvido como byte[]. O relatorio de uma
 * analise tem poucas paginas; gravar em disco so criaria arquivo temporario
 * para limpar depois.
 */
@Component
public class AnalysisReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisReportGenerator.class);

    private static final PDType1Font TITULO = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font CORPO = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font DESTAQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    public byte[] generate(AnalysisResponse analise) {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {

            // O writer precisa ser fechado ANTES do save: o PDFBox so grava o
            // conteudo da pagina quando o content stream e encerrado.
            try (PdfTextWriter writer = new PdfTextWriter(documento)) {
                escrever(writer, analise);
            }

            documento.save(saida);
            return saida.toByteArray();

        } catch (IOException ex) {
            log.error("Falha ao gerar o PDF da analise {}", analise.id(), ex);
            throw new ReportGenerationException(ex);
        }
    }

    private void escrever(PdfTextWriter writer, AnalysisResponse analise) throws IOException {
        writer.paragrafo("Relatorio de Analise de Curriculo", TITULO, 18, 4);
        writer.paragrafo("Gerado em " + DATA_HORA.format(analise.analisadoEm()), CORPO, 9, 6);
        writer.linhaHorizontal();

        writer.paragrafo("Candidato: " + analise.candidato(), CORPO, 11, 2);
        writer.paragrafo("Vaga: " + analise.vaga(), CORPO, 11, 10);

        writer.paragrafo("Compatibilidade: " + analise.compatibilidade() + "%", DESTAQUE, 14, 6);
        writer.barra(analise.compatibilidade(), 12);
        writer.paragrafo("%d de %d requisitos atendidos".formatted(
                analise.pontosFortes().size(), analise.totalRequisitos()), CORPO, 10, 14);

        secao(writer, "Pontos fortes", analise.pontosFortes(),
                "Nenhum requisito da vaga foi encontrado no curriculo.");
        secao(writer, "Requisitos ausentes", analise.requisitosAusentes(),
                "Nenhum. O curriculo atende a todos os requisitos.");
        secao(writer, "Recomendacoes", analise.recomendacoes(),
                "Sem recomendacoes para esta analise.");

        writer.linhaHorizontal();
        writer.paragrafo("Analise " + analise.id(), CORPO, 8, 0);
        writer.paragrafo("CV Analyzer API", CORPO, 8, 0);
    }

    /**
     * Secao com titulo e lista. Lista vazia nao desaparece: vira uma frase
     * explicando o vazio. Relatorio com secao faltando parece relatorio
     * quebrado, mesmo quando o dado e legitimamente vazio.
     */
    private void secao(PdfTextWriter writer, String titulo, List<String> itens, String textoVazio)
            throws IOException {

        writer.paragrafo(titulo, DESTAQUE, 12, 4);

        if (itens == null || itens.isEmpty()) {
            writer.paragrafo(textoVazio, CORPO, 10, 12);
            return;
        }

        for (String item : itens) {
            writer.item(item, CORPO, 10);
        }
        writer.avancar(12);
    }
}
