package com.portfolio.cvanalyzer.analysis.engine;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gera recomendacoes praticas a partir dos requisitos que faltam.
 *
 * Duas fontes de texto:
 *  1. `recommendations.csv`, com uma sugestao especifica por tecnologia;
 *  2. um texto generico, para tecnologia fora do catalogo.
 *
 * Nao ha IA aqui — e um mapa de sugestoes. A vantagem e ser deterministico,
 * instantaneo e de graca; a limitacao e nao entender contexto. Quando o
 * modulo de IA entrar, ele substitui esta classe atras da mesma interface
 * e nada mais no sistema muda.
 */
@Component
public class RecommendationEngine {

    private static final Logger log = LoggerFactory.getLogger(RecommendationEngine.class);
    private static final String ARQUIVO = "recommendations.csv";

    /** Acima disso a lista vira parede de texto e ninguem le. */
    private static final int MAX_RECOMENDACOES = 6;

    /** Abaixo desta nota vale sugerir foco antes de sair estudando tudo. */
    private static final int NOTA_BAIXA = 50;
    private static final int NOTA_ALTA = 80;

    private final Map<String, String> sugestoes = new HashMap<>();

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
                String[] partes = limpa.split(";", 2);
                if (partes.length == 2 && !partes[0].isBlank() && !partes[1].isBlank()) {
                    sugestoes.put(partes[0].trim(), partes[1].trim());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar " + ARQUIVO, e);
        }
        log.info("Motor de recomendacoes carregado com {} sugestoes", sugestoes.size());
    }

    public List<String> generate(int compatibilidade, List<String> ausentes, List<String> atendidos) {
        if (ausentes.isEmpty()) {
            return List.of(mensagemDeCandidatoCompleto(compatibilidade, atendidos));
        }

        List<String> recomendacoes = new ArrayList<>();
        recomendacoes.add(mensagemDeAbertura(compatibilidade, ausentes));

        ausentes.stream()
                .limit(MAX_RECOMENDACOES - 1L)
                .map(this::sugestaoPara)
                .forEach(recomendacoes::add);

        int naoListados = ausentes.size() - (MAX_RECOMENDACOES - 1);
        if (naoListados > 0) {
            recomendacoes.add("Ha mais %d requisito(s) da vaga fora do seu curriculo; "
                    .formatted(naoListados)
                    + "priorize os listados acima antes de avancar para os demais.");
        }
        return List.copyOf(recomendacoes);
    }

    private String sugestaoPara(String tecnologia) {
        return sugestoes.getOrDefault(tecnologia,
                "Estude %s e registre o aprendizado em um projeto publico no seu portfolio"
                        .formatted(tecnologia));
    }

    private String mensagemDeAbertura(int compatibilidade, List<String> ausentes) {
        if (compatibilidade < NOTA_BAIXA) {
            return ("Sua compatibilidade com esta vaga e de %d%%. "
                    + "Concentre-se primeiro em %s, que e o requisito de maior peso ausente.")
                    .formatted(compatibilidade, ausentes.getFirst());
        }
        if (compatibilidade < NOTA_ALTA) {
            return ("Sua compatibilidade com esta vaga e de %d%%. "
                    + "Voce ja atende boa parte dos requisitos; cobrir os %d itens abaixo "
                    + "deixaria seu perfil bem mais competitivo.")
                    .formatted(compatibilidade, ausentes.size());
        }
        return ("Sua compatibilidade com esta vaga e de %d%%, um bom resultado. "
                + "Faltam poucos ajustes para o perfil ficar completo.")
                .formatted(compatibilidade);
    }

    private String mensagemDeCandidatoCompleto(int compatibilidade, List<String> atendidos) {
        return ("Voce atende a todos os %d requisitos desta vaga (%d%% de compatibilidade). "
                + "Destaque essas tecnologias logo no inicio do curriculo e "
                + "descreva resultados concretos obtidos com elas.")
                .formatted(atendidos.size(), compatibilidade);
    }
}
