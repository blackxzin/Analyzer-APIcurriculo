package com.portfolio.cvanalyzer.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Gera PDFs de verdade para os testes.
 *
 * Melhor do que versionar um .pdf binario no repositorio: o conteudo fica
 * visivel no codigo, da para variar o texto por teste e ninguem precisa
 * abrir um arquivo para saber o que esta sendo testado.
 */
public final class PdfTestFactory {

    private static final float MARGEM_ESQUERDA = 50;
    private static final float TOPO = 780;
    private static final float ALTURA_LINHA = 14;

    private PdfTestFactory() {
    }

    public static byte[] pdfComTexto(List<String> linhas) {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {

            PDPage pagina = new PDPage();
            documento.addPage(pagina);

            try (PDPageContentStream stream = new PDPageContentStream(documento, pagina)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                stream.newLineAtOffset(MARGEM_ESQUERDA, TOPO);
                for (String linha : linhas) {
                    // As fontes Standard 14 nao tem acentuacao completa;
                    // trocamos por ASCII para o PDF nao estourar no teste.
                    stream.showText(paraAscii(linha));
                    stream.newLineAtOffset(0, -ALTURA_LINHA);
                }
                stream.endText();
            }

            documento.save(saida);
            return saida.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar PDF de teste", e);
        }
    }

    private static String paraAscii(String texto) {
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * PDF com N paginas, cada uma com o mesmo texto.
     * O gerador de uma pagina so nao serve aqui: ele escreve todas as linhas
     * na mesma pagina, entao o documento continuaria com pageCount = 1.
     */
    public static byte[] pdfComPaginas(int quantidade, String texto) {
        try (PDDocument documento = new PDDocument();
             ByteArrayOutputStream saida = new ByteArrayOutputStream()) {

            for (int i = 0; i < quantidade; i++) {
                PDPage pagina = new PDPage();
                documento.addPage(pagina);
                try (PDPageContentStream stream = new PDPageContentStream(documento, pagina)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    stream.newLineAtOffset(MARGEM_ESQUERDA, TOPO);
                    stream.showText(paraAscii(texto));
                    stream.endText();
                }
            }

            documento.save(saida);
            return saida.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar PDF de teste", e);
        }
    }

    /** Curriculo de exemplo usado na maior parte dos testes. */
    public static byte[] curriculoExemplo() {
        return pdfComTexto(List.of(
                "Maria Souza Lima",
                "maria.souza@email.com | (11) 98765-4321",
                "",
                "RESUMO",
                "Desenvolvedora backend com foco em Java.",
                "",
                "EXPERIENCIA PROFISSIONAL",
                "- Desenvolvedora Java na Acme (2021 - 2024), atuando com Java, SQL e Git",
                "- Estagiaria de TI na Beta (2020 - 2021)",
                "",
                "FORMACAO ACADEMICA",
                "- Bacharelado em Ciencia da Computacao - USP (2016 - 2020)",
                "",
                "CURSOS",
                "- Algoritmos e Estruturas de Dados - Coursera",
                "",
                "CERTIFICACOES",
                "- Oracle Certified Associate Java SE 8",
                "",
                "IDIOMAS",
                "- Portugues: nativo",
                "- Ingles: avancado"));
    }
}
