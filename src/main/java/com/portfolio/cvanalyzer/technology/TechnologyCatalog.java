package com.portfolio.cvanalyzer.technology;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dicionario de tecnologias reconhecidas pela aplicacao.
 *
 * Carrega `technologies.csv` uma unica vez na subida e compila os regex de
 * cada apelido. Compilar Pattern e caro; fazer isso a cada analise seria
 * desperdicio, entao pagamos o custo uma vez so.
 *
 * Por que regex com fronteira propria em vez de `texto.contains(termo)`?
 *  - "contains" acha "Java" dentro de "JavaScript" (falso positivo);
 *  - \b do Java nao funciona para "C++", "C#" e ".NET", porque + # . nao
 *    sao caracteres de palavra. Por isso definimos a fronteira na mao.
 */
@Component
public class TechnologyCatalog {

    private static final Logger log = LoggerFactory.getLogger(TechnologyCatalog.class);
    private static final String ARQUIVO = "technologies.csv";

    /**
     * Fronteiras do termo. Sao ASSIMETRICAS de proposito:
     *
     * ANTES do termo o ponto conta como parte da palavra — assim "js" dentro
     * de "node.js" nao vira JavaScript, e ".net" dentro de "asp.net" nao vira
     * .NET solto.
     *
     * DEPOIS do termo o ponto NAO conta, senao "Docker." no fim de uma frase
     * deixaria de ser reconhecido.
     */
    private static final String FRONTEIRA_ESQUERDA = "[a-z0-9+#._-]";
    private static final String FRONTEIRA_DIREITA = "[a-z0-9+#_-]";

    private final List<Entrada> entradas = new ArrayList<>();

    private record Entrada(String canonico, Pattern padrao) {
    }

    @PostConstruct
    void carregar() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(ARQUIVO).getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            while ((linha = reader.readLine()) != null) {
                String limpa = linha.trim();
                if (limpa.isEmpty() || limpa.startsWith("#")) {
                    continue;
                }
                adicionar(limpa);
            }
        } catch (IOException e) {
            // Sem catalogo a aplicacao nao consegue analisar nada.
            // Falhar na subida e melhor do que responder errado em producao.
            throw new UncheckedIOException("Falha ao carregar " + ARQUIVO, e);
        }

        // Apelido mais longo primeiro: garante que "spring boot" seja testado
        // antes de "spring", senao a tecnologia mais especifica nunca vence.
        entradas.sort(Comparator.comparingInt(
                (Entrada e) -> e.padrao().pattern().length()).reversed());

        if (entradas.isEmpty()) {
            throw new IllegalStateException(ARQUIVO + " nao contem nenhuma tecnologia valida");
        }
        log.info("Catalogo carregado com {} padroes de tecnologia", entradas.size());
    }

    private void adicionar(String linha) {
        String[] partes = linha.split(";", 2);
        String canonico = partes[0].trim();
        if (canonico.isEmpty()) {
            return;
        }

        Set<String> apelidos = new LinkedHashSet<>();
        apelidos.add(TextNormalizer.normalize(canonico));
        if (partes.length > 1) {
            for (String apelido : partes[1].split(",")) {
                String normalizado = TextNormalizer.normalize(apelido);
                if (!normalizado.isEmpty()) {
                    apelidos.add(normalizado);
                }
            }
        }

        for (String apelido : apelidos) {
            entradas.add(new Entrada(canonico, compilar(apelido)));
        }
    }

    private Pattern compilar(String apelido) {
        // \s+ entre palavras: aceita "spring   boot" e quebra de linha no PDF.
        String corpo = Pattern.quote(apelido).replace(" ", "\\E\\s+\\Q");
        return Pattern.compile(
                "(?<!" + FRONTEIRA_ESQUERDA + ")" + corpo + "(?!" + FRONTEIRA_DIREITA + ")");
    }

    /**
     * Devolve os nomes canonicos de todas as tecnologias citadas no texto.
     * A ordem segue a do catalogo e nao repete.
     */
    public Set<String> extractFrom(String texto) {
        String normalizado = TextNormalizer.normalize(texto);
        if (normalizado.isEmpty()) {
            return Set.of();
        }

        Set<String> encontradas = new LinkedHashSet<>();
        for (Entrada entrada : entradas) {
            if (encontradas.contains(entrada.canonico())) {
                continue;
            }
            Matcher matcher = entrada.padrao().matcher(normalizado);
            if (matcher.find()) {
                encontradas.add(entrada.canonico());
            }
        }
        return encontradas;
    }

    /**
     * Traduz um termo solto para o nome canonico do catalogo.
     * Usado quando o usuario digita requisitos da vaga na mao: "springboot"
     * e "Spring Boot" precisam virar a MESMA coisa, senao a comparacao falha.
     * Termo desconhecido volta como veio, apenas aparado.
     */
    public String canonicalize(String termo) {
        return findCanonical(termo).orElseGet(termo::trim);
    }

    public Optional<String> findCanonical(String termo) {
        String normalizado = TextNormalizer.normalize(termo);
        if (normalizado.isEmpty()) {
            return Optional.empty();
        }
        return entradas.stream()
                .filter(e -> e.padrao().matcher(normalizado).matches())
                .map(Entrada::canonico)
                .findFirst();
    }
}
