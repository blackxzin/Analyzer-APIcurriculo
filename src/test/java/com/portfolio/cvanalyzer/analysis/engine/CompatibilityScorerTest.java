package com.portfolio.cvanalyzer.analysis.engine;

import com.portfolio.cvanalyzer.job.Job;
import com.portfolio.cvanalyzer.job.Seniority;
import com.portfolio.cvanalyzer.resume.Resume;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A regra de pontuacao e o coracao do produto: se ela erra, todo o resto
 * do sistema entrega um numero errado com cara de verdade. Por isso o
 * teste cobre cada caminho, incluindo os extremos.
 *
 * Nenhum Spring, nenhum banco — so objetos.
 */
class CompatibilityScorerTest {

    private final CompatibilityScorer scorer = new CompatibilityScorer();

    private Resume curriculoCom(String... tecnologias) {
        Resume resume = Resume.create("cv.pdf", 1024L, 1, "texto do curriculo");
        resume.replaceTechnologies(Set.of(tecnologias));
        return resume;
    }

    private Job vagaCom(List<String> obrigatorios, List<String> desejaveis) {
        Job job = Job.create("Dev Java", "Acme", Seniority.PLENO, "descricao da vaga");
        job.replaceRequirements(obrigatorios, desejaveis);
        return job;
    }

    @Test
    @DisplayName("candidato que atende tudo pontua 100")
    void atendeTudo() {
        CompatibilityScore score = scorer.score(
                curriculoCom("Java", "Docker"),
                vagaCom(List.of("Java", "Docker"), List.of()));

        assertThat(score.percentage()).isEqualTo(100);
        assertThat(score.matched()).containsExactly("Java", "Docker");
        assertThat(score.missing()).isEmpty();
    }

    @Test
    @DisplayName("candidato que nao atende nada pontua 0")
    void naoAtendeNada() {
        CompatibilityScore score = scorer.score(
                curriculoCom("PHP"),
                vagaCom(List.of("Java", "Docker"), List.of()));

        assertThat(score.percentage()).isZero();
        assertThat(score.matched()).isEmpty();
        assertThat(score.missing()).containsExactly("Java", "Docker");
    }

    @Test
    @DisplayName("requisito obrigatorio pesa 3x mais que desejavel")
    void obrigatorioPesaMais() {
        // Vaga: 1 obrigatorio (peso 3) + 1 desejavel (peso 1) = peso total 4.
        Job vaga = vagaCom(List.of("Java"), List.of("Kubernetes"));

        CompatibilityScore soObrigatorio = scorer.score(curriculoCom("Java"), vaga);
        CompatibilityScore soDesejavel = scorer.score(curriculoCom("Kubernetes"), vaga);

        assertThat(soObrigatorio.percentage()).isEqualTo(75);  // 3/4
        assertThat(soDesejavel.percentage()).isEqualTo(25);    // 1/4
        assertThat(soObrigatorio.percentage()).isGreaterThan(soDesejavel.percentage());
    }

    @Test
    @DisplayName("arredonda para o inteiro mais proximo, sem truncar")
    void arredondaCorretamente() {
        // 2 de 3 obrigatorios = 6/9 = 66,67% -> 67, e nao 66.
        CompatibilityScore score = scorer.score(
                curriculoCom("Java", "SQL"),
                vagaCom(List.of("Java", "SQL", "Docker"), List.of()));

        assertThat(score.percentage()).isEqualTo(67);
    }

    @Test
    @DisplayName("tecnologia do curriculo que a vaga nao pede nao infla a nota")
    void tecnologiaExtraNaoConta() {
        CompatibilityScore score = scorer.score(
                curriculoCom("Java", "Python", "Rust", "Go", "Scala"),
                vagaCom(List.of("Java", "Docker"), List.of()));

        assertThat(score.percentage()).isEqualTo(50);
        assertThat(score.matched()).containsExactly("Java");
    }

    @Test
    @DisplayName("informa o total de requisitos avaliados")
    void informaTotal() {
        CompatibilityScore score = scorer.score(
                curriculoCom("Java"),
                vagaCom(List.of("Java", "Docker"), List.of("Kafka")));

        assertThat(score.totalRequirements()).isEqualTo(3);
    }

    @Test
    @DisplayName("vaga sem requisitos devolve 0 em vez de dividir por zero")
    void vagaSemRequisitos() {
        CompatibilityScore score = scorer.score(
                curriculoCom("Java"),
                vagaCom(List.of(), List.of()));

        assertThat(score.percentage()).isZero();
        assertThat(score.totalRequirements()).isZero();
    }

    @Test
    @DisplayName("curriculo sem nenhuma tecnologia nao quebra")
    void curriculoVazio() {
        CompatibilityScore score = scorer.score(
                curriculoCom(),
                vagaCom(List.of("Java"), List.of()));

        assertThat(score.percentage()).isZero();
        assertThat(score.missing()).containsExactly("Java");
    }

    @Test
    @DisplayName("cenario realista com obrigatorios e desejaveis misturados")
    void cenarioRealista() {
        // Obrigatorios: Java, SQL e Git atendidos, Spring Boot ausente
        //               -> 9 de 12 pontos (peso 3 cada)
        // Desejaveis:   Linux, Maven e JUnit atendidos, Docker ausente
        //               -> 3 de 4 pontos (peso 1 cada)
        // Nota: (9 + 3) / (12 + 4) = 12/16 = 75%
        CompatibilityScore score = scorer.score(
                curriculoCom("Java", "SQL", "Git", "Linux", "Maven", "JUnit"),
                vagaCom(List.of("Java", "SQL", "Git", "Spring Boot"),
                        List.of("Linux", "Maven", "JUnit", "Docker")));

        assertThat(score.percentage()).isEqualTo(75);
        assertThat(score.matched()).contains("Java", "SQL", "Git");
        assertThat(score.missing()).containsExactly("Spring Boot", "Docker");
    }
}
