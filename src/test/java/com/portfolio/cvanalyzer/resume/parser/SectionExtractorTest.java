package com.portfolio.cvanalyzer.resume.parser;

import com.portfolio.cvanalyzer.resume.SectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SectionExtractorTest {

    private final SectionExtractor extractor = new SectionExtractor();

    private static final String CURRICULO = """
            Maria Souza Lima
            maria@email.com

            OBJETIVO
            Atuar como desenvolvedora backend.

            EXPERIÊNCIA PROFISSIONAL
            - Desenvolvedora Java na Acme (2021 - 2024)
            - Estagiária de TI na Beta (2020 - 2021)

            FORMAÇÃO ACADÊMICA
            - Bacharelado em Ciência da Computação - USP

            CURSOS
            - Algoritmos e Estruturas de Dados

            CERTIFICAÇÕES
            - Oracle Certified Associate

            IDIOMAS
            - Português: nativo
            - Inglês: avançado
            """;

    @Test
    @DisplayName("separa todas as secoes conhecidas")
    void separaSecoes() {
        Map<SectionType, List<String>> secoes = extractor.extract(CURRICULO);

        assertThat(secoes.get(SectionType.EXPERIENCE)).hasSize(2)
                .anySatisfy(item -> assertThat(item).contains("Desenvolvedora Java na Acme"));
        assertThat(secoes.get(SectionType.EDUCATION)).hasSize(1);
        assertThat(secoes.get(SectionType.COURSE)).hasSize(1);
        assertThat(secoes.get(SectionType.CERTIFICATION)).hasSize(1);
        assertThat(secoes.get(SectionType.LANGUAGE)).hasSize(2);
    }

    @Test
    @DisplayName("o conteudo de OBJETIVO nao vaza para outra secao")
    void naoVazaSecaoNeutra() {
        Map<SectionType, List<String>> secoes = extractor.extract(CURRICULO);
        assertThat(secoes.values())
                .allSatisfy(itens -> assertThat(itens)
                        .noneMatch(item -> item.contains("Atuar como desenvolvedora")));
    }

    @Test
    @DisplayName("remonta linha quebrada pelo PDF em um item so")
    void remontaLinhaQuebrada() {
        String texto = """
                EXPERIÊNCIA
                - Desenvolvedora backend responsavel por
                  APIs REST e integracao com sistemas
                  legados da empresa

                - Analista de suporte
                """;
        List<String> experiencias = extractor.extract(texto).get(SectionType.EXPERIENCE);

        assertThat(experiencias).hasSize(2);
        assertThat(experiencias.getFirst())
                .contains("APIs REST")
                .contains("legados da empresa");
    }

    @Test
    @DisplayName("junta 'Cursos' e 'Formacao complementar' na mesma secao")
    void juntaSecoesEquivalentes() {
        String texto = """
                CURSOS
                - Curso A

                FORMAÇÃO COMPLEMENTAR
                - Curso B
                """;
        assertThat(extractor.extract(texto).get(SectionType.COURSE))
                .containsExactly("Curso A", "Curso B");
    }

    @Test
    @DisplayName("reconhece cabecalho com pontuacao ao redor")
    void cabecalhoComPontuacao() {
        String texto = """
                — IDIOMAS —
                Ingles avancado
                """;
        assertThat(extractor.extract(texto).get(SectionType.LANGUAGE))
                .containsExactly("Ingles avancado");
    }

    @Test
    @DisplayName("texto sem cabecalho nenhum devolve mapa vazio")
    void semCabecalhos() {
        assertThat(extractor.extract("Apenas um texto solto sem estrutura")).isEmpty();
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}
