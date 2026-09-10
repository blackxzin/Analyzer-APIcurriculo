package com.portfolio.cvanalyzer.resume.parser;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encontra o telefone do candidato, no formato brasileiro.
 *
 * Estrategia em dois passos:
 *  1. regex larga, pega qualquer coisa com cara de telefone;
 *  2. filtro por quantidade de digitos.
 *
 * O passo 2 e o que impede confundir telefone com ano ("2020 - 2024"),
 * CEP ou numero de documento — todos tem contagem de digitos diferente.
 */
@Component
public class PhoneExtractor {

    /*
     * (?<!\d) e (?!\d) sao essenciais.
     *
     * Sem eles, em "Telefone: 11987654321" o regex casa a partir do ESPACO
     * (o separador opcional aceita espaco) e para em 8 digitos, devolvendo
     * "11987654" — numero truncado. O regex prefere o match mais a esquerda,
     * nao o mais longo. As duas guardas obrigam o match a comecar e terminar
     * em fronteira de digito.
     */
    private static final Pattern CANDIDATO = Pattern.compile(
            "(?<!\\d)(?:\\+\\s?\\d{1,3}[\\s.\\-]?)?(?:\\(\\s?\\d{2}\\s?\\)|\\d{2})?"
                    + "[\\s.\\-]?9?\\d{4}[\\s.\\-]?\\d{4}(?!\\d)");

    private static final Pattern NAO_DIGITO = Pattern.compile("\\D");

    private static final int MIN_DIGITOS = 10;  // fixo com DDD
    private static final int MAX_DIGITOS = 13;  // +55 + DDD + 9 digitos

    public Optional<String> extract(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = CANDIDATO.matcher(texto);
        while (matcher.find()) {
            String bruto = matcher.group().trim();
            int digitos = NAO_DIGITO.matcher(bruto).replaceAll("").length();
            if (digitos >= MIN_DIGITOS && digitos <= MAX_DIGITOS) {
                return Optional.of(bruto);
            }
        }
        return Optional.empty();
    }
}
