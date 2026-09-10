package com.portfolio.cvanalyzer.analysis.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationEngineTest {

    private RecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RecommendationEngine();
        engine.carregar();
    }

    @Test
    @DisplayName("usa a sugestao especifica quando a tecnologia esta no catalogo")
    void sugestaoEspecifica() {
        List<String> recomendacoes = engine.generate(60, List.of("Docker"), List.of("Java"));

        assertThat(recomendacoes).anySatisfy(item ->
                assertThat(item).containsIgnoringCase("Dockerfile"));
    }

    @Test
    @DisplayName("usa texto generico para tecnologia fora do catalogo")
    void sugestaoGenerica() {
        List<String> recomendacoes = engine.generate(40, List.of("Cobol"), List.of());

        assertThat(recomendacoes).anySatisfy(item ->
                assertThat(item).contains("Cobol").contains("portfolio"));
    }

    @Test
    @DisplayName("candidato sem lacunas recebe mensagem de perfil completo")
    void semLacunas() {
        List<String> recomendacoes = engine.generate(100, List.of(), List.of("Java", "Docker"));

        assertThat(recomendacoes).hasSize(1);
        assertThat(recomendacoes.getFirst()).contains("todos os 2 requisitos");
    }

    @Test
    @DisplayName("nota baixa manda focar no requisito de maior peso ausente")
    void notaBaixaPriorizaFoco() {
        List<String> recomendacoes = engine.generate(20, List.of("Java", "Docker"), List.of());

        assertThat(recomendacoes.getFirst())
                .contains("20%")
                .contains("Java");
    }

    @Test
    @DisplayName("limita a quantidade de recomendacoes e avisa quantas sobraram")
    void limitaQuantidade() {
        List<String> ausentes = List.of("Java", "Docker", "Kafka", "Redis",
                "Kubernetes", "Terraform", "AWS", "GraphQL");

        List<String> recomendacoes = engine.generate(30, ausentes, List.of());

        assertThat(recomendacoes).hasSizeLessThanOrEqualTo(7);
        assertThat(recomendacoes.getLast()).contains("mais 3 requisito");
    }
}
