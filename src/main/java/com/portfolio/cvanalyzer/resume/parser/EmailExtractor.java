package com.portfolio.cvanalyzer.resume.parser;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encontra o e-mail do candidato no texto do curriculo.
 */
@Component
public class EmailExtractor {

    /**
     * Regex deliberadamente mais restrita que a RFC 5322.
     * A RFC completa aceita coisas que nenhum curriculo real usa e a regex
     * fica ilegivel. Aqui queremos achar o e-mail obvio, nao validar
     * endereco exotico.
     */
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    public Optional<String> extract(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = EMAIL.matcher(texto);
        if (!matcher.find()) {
            return Optional.empty();
        }
        // Minusculas: e-mail nao diferencia caixa no dominio e queremos
        // que o mesmo candidato sempre grave a mesma string.
        return Optional.of(matcher.group().toLowerCase(Locale.ROOT));
    }
}
