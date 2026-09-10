package com.portfolio.cvanalyzer.technology;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Normaliza texto para comparacao.
 *
 * Problema real: o curriculo escreve "Programação Orientada a Objetos",
 * a vaga escreve "programacao orientada a objetos". Sem normalizar,
 * sao strings diferentes e o match falha.
 *
 * Passos: minusculas -> remove acentos -> colapsa espacos.
 *
 * Classe utilitaria: construtor privado, ninguem instancia.
 */
public final class TextNormalizer {

    private static final Pattern ACENTOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private TextNormalizer() {
        throw new AssertionError("Classe utilitaria nao deve ser instanciada");
    }

    public static String normalize(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String semAcento = ACENTOS.matcher(
                Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return ESPACOS.matcher(semAcento.toLowerCase()).replaceAll(" ").trim();
    }
}
