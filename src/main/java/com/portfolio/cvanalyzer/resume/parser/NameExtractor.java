package com.portfolio.cvanalyzer.resume.parser;

import com.portfolio.cvanalyzer.technology.TextNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Descobre o nome do candidato.
 *
 * Nao existe campo "nome" num PDF — e preciso deduzir. A heuristica usada:
 * o nome quase sempre esta nas primeiras linhas, e uma linha curta, so com
 * letras, com 2 a 6 palavras iniciadas em maiuscula, e nao e um titulo
 * generico como "CURRICULO".
 *
 * Heuristica erra as vezes; por isso o retorno e Optional e o campo
 * `candidate_name` no banco aceita nulo. Preferimos admitir "nao sei" a
 * gravar lixo.
 */
@Component
public class NameExtractor {

    private static final int LINHAS_ANALISADAS = 12;
    private static final int MIN_PALAVRAS = 2;
    private static final int MAX_PALAVRAS = 6;
    private static final int MAX_CARACTERES = 60;

    /** Palavra com inicial maiuscula, acentos aceitos. Tambem aceita TUDO MAIUSCULO. */
    private static final Pattern PALAVRA_DE_NOME =
            Pattern.compile("[\\p{Lu}][\\p{Ll}'\\-]+|[\\p{Lu}'\\-]{2,}");

    /** Conectivos que aparecem em nomes brasileiros e nao comecam com maiuscula. */
    private static final Set<String> CONECTIVOS = Set.of("de", "da", "do", "das", "dos", "e");

    /** Linhas de cabecalho que nao sao nome. */
    private static final Set<String> TITULOS_GENERICOS = Set.of(
            "curriculo", "curriculum", "curriculum vitae", "cv", "resume",
            "dados pessoais", "informacoes pessoais", "perfil", "contato", "resumo");

    public Optional<String> extract(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }

        List<String> primeirasLinhas = texto.lines()
                .map(String::strip)
                .filter(linha -> !linha.isEmpty())
                .limit(LINHAS_ANALISADAS)
                .toList();

        return primeirasLinhas.stream()
                .filter(this::pareceNome)
                .findFirst();
    }

    private boolean pareceNome(String linha) {
        if (linha.length() > MAX_CARACTERES) {
            return false;
        }
        // Linha de contato nunca e a linha do nome.
        if (linha.contains("@") || linha.contains("http") || linha.matches(".*\\d.*")) {
            return false;
        }
        if (TITULOS_GENERICOS.contains(TextNormalizer.normalize(linha))) {
            return false;
        }

        String[] palavras = linha.split("\\s+");
        if (palavras.length < MIN_PALAVRAS || palavras.length > MAX_PALAVRAS) {
            return false;
        }

        for (String palavra : palavras) {
            if (CONECTIVOS.contains(TextNormalizer.normalize(palavra))) {
                continue;
            }
            if (!PALAVRA_DE_NOME.matcher(palavra).matches()) {
                return false;
            }
        }
        return true;
    }
}
