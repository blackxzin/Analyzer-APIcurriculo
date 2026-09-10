package com.portfolio.cvanalyzer.resume.pdf;

/**
 * Resultado bruto da leitura do PDF, antes de qualquer interpretacao.
 */
public record ExtractedPdf(String text, int pageCount) {
}
