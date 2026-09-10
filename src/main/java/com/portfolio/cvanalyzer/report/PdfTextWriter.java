package com.portfolio.cvanalyzer.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Escrita de texto em PDF com quebra de linha e de pagina automaticas.
 *
 * O PDFBox trabalha em coordenadas absolutas: ele desenha um texto no ponto
 * (x, y) e nao sabe o que e paragrafo, linha cheia ou fim de pagina. Esta
 * classe guarda o cursor vertical, mede cada palavra na fonte escolhida e
 * decide sozinha quando quebrar a linha e quando comecar outra pagina.
 *
 * Fica separada do gerador do relatorio de proposito: aqui mora COMO se
 * escreve num PDF, la mora O QUE o relatorio diz.
 */
class PdfTextWriter implements AutoCloseable {

    private static final PDRectangle TAMANHO_PAGINA = PDRectangle.A4;
    private static final float MARGEM = 50f;
    private static final float LARGURA_UTIL = TAMANHO_PAGINA.getWidth() - (2 * MARGEM);
    private static final float TOPO = TAMANHO_PAGINA.getHeight() - MARGEM;
    private static final float RODAPE = MARGEM;

    private final PDDocument document;
    private PDPageContentStream stream;
    private float cursorY;

    PdfTextWriter(PDDocument document) throws IOException {
        this.document = document;
        novaPagina();
    }

    float larguraUtil() {
        return LARGURA_UTIL;
    }

    /** Escreve um paragrafo, quebrando em quantas linhas forem necessarias. */
    void paragrafo(String texto, PDFont fonte, float tamanho, float espacoDepois) throws IOException {
        for (String linha : quebrarEmLinhas(limpar(texto), fonte, tamanho, LARGURA_UTIL)) {
            escreverLinha(linha, fonte, tamanho, MARGEM);
        }
        avancar(espacoDepois);
    }

    /**
     * Item de lista. A continuacao da linha entra alinhada com o texto, e nao
     * embaixo do marcador — detalhe pequeno que separa relatorio legivel de
     * texto derramado na pagina.
     */
    void item(String texto, PDFont fonte, float tamanho) throws IOException {
        float recuo = 14f;
        List<String> linhas = quebrarEmLinhas(limpar(texto), fonte, tamanho, LARGURA_UTIL - recuo);

        for (int i = 0; i < linhas.size(); i++) {
            String prefixo = (i == 0) ? "- " : "  ";
            escreverLinha(prefixo + linhas.get(i), fonte, tamanho, MARGEM + (i == 0 ? 0 : recuo));
        }
    }

    /** Barra horizontal preenchida ate a porcentagem informada. */
    void barra(int percentual, float altura) throws IOException {
        float largura = LARGURA_UTIL;
        garantirEspaco(altura + 6);
        float y = cursorY - altura;

        stream.setNonStrokingColor(0.90f);
        stream.addRect(MARGEM, y, largura, altura);
        stream.fill();

        stream.setNonStrokingColor(0.15f);
        stream.addRect(MARGEM, y, largura * (Math.clamp(percentual, 0, 100) / 100f), altura);
        stream.fill();

        stream.setNonStrokingColor(0f);
        cursorY = y - 6;
    }

    void linhaHorizontal() throws IOException {
        garantirEspaco(10);
        stream.setStrokingColor(0.75f);
        stream.moveTo(MARGEM, cursorY);
        stream.lineTo(MARGEM + LARGURA_UTIL, cursorY);
        stream.stroke();
        stream.setStrokingColor(0f);
        cursorY -= 10;
    }

    void avancar(float pontos) {
        cursorY -= pontos;
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }

    private void escreverLinha(String linha, PDFont fonte, float tamanho, float x) throws IOException {
        float alturaLinha = tamanho * 1.35f;
        garantirEspaco(alturaLinha);

        stream.beginText();
        stream.setFont(fonte, tamanho);
        stream.newLineAtOffset(x, cursorY - tamanho);
        stream.showText(linha);
        stream.endText();

        cursorY -= alturaLinha;
    }

    private void garantirEspaco(float necessario) throws IOException {
        if (cursorY - necessario < RODAPE) {
            novaPagina();
        }
    }

    private void novaPagina() throws IOException {
        close();
        PDPage pagina = new PDPage(TAMANHO_PAGINA);
        document.addPage(pagina);
        stream = new PDPageContentStream(document, pagina);
        cursorY = TOPO;
    }

    private List<String> quebrarEmLinhas(String texto, PDFont fonte, float tamanho, float largura)
            throws IOException {

        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();

        for (String palavra : texto.split("\\s+")) {
            String candidata = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (largura(candidata, fonte, tamanho) <= largura || atual.isEmpty()) {
                atual.setLength(0);
                atual.append(candidata);
            } else {
                linhas.add(atual.toString());
                atual.setLength(0);
                atual.append(palavra);
            }
        }

        if (!atual.isEmpty()) {
            linhas.add(atual.toString());
        }
        return linhas.isEmpty() ? List.of("") : linhas;
    }

    private float largura(String texto, PDFont fonte, float tamanho) throws IOException {
        return fonte.getStringWidth(texto) / 1000 * tamanho;
    }

    /**
     * As fontes padrao do PDF usam a tabela WinAnsi, que nao cobre todo o
     * Unicode: um caractere de fora dela faz o PDFBox lancar excecao no meio
     * da escrita. Como o texto vem de curriculo enviado por terceiro, ele
     * pode conter qualquer coisa. Entao tiramos os acentos (a -> a) e
     * trocamos o que sobrar de estranho por '?'. Relatorio sem acento e
     * aceitavel; endpoint que estoura 500 por causa de um simbolo, nao.
     */
    private String limpar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        StringBuilder resultado = new StringBuilder(semAcento.length());
        for (char c : semAcento.toCharArray()) {
            resultado.append(c == '\n' || c == '\t' ? ' ' : (c >= 32 && c <= 255 ? c : '?'));
        }
        return resultado.toString().trim();
    }
}
