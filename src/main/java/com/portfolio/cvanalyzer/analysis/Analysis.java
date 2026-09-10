package com.portfolio.cvanalyzer.analysis;

import com.portfolio.cvanalyzer.job.Job;
import com.portfolio.cvanalyzer.resume.Resume;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resultado de uma comparacao curriculo x vaga.
 *
 * DECISAO IMPORTANTE: o resultado fica CONGELADO aqui, com as listas de
 * requisitos atendidos, ausentes e as recomendacoes copiadas.
 *
 * A alternativa seria guardar so os ids e recalcular na hora de exibir.
 * Nao serve: se a vaga mudar os requisitos amanha, todo o historico passaria
 * a mostrar numeros diferentes dos que o usuario viu no dia. Historico que
 * muda sozinho nao e historico.
 */
@Entity
@Table(name = "analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private int compatibility;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount;

    @Column(name = "total_requirements", nullable = false)
    private int totalRequirements;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_matched_requirement",
            joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "requirement", nullable = false, length = 80)
    private Set<String> matchedRequirements = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_missing_requirement",
            joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "requirement", nullable = false, length = 80)
    private Set<String> missingRequirements = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analysis_recommendation",
            joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(name = "recommendation", nullable = false, columnDefinition = "text")
    @OrderColumn(name = "sort_order")
    private List<String> recommendations = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private Analysis(Resume resume, Job job, int compatibility, int totalRequirements) {
        this.resume = resume;
        this.job = job;
        this.compatibility = compatibility;
        this.totalRequirements = totalRequirements;
    }

    public static Analysis create(Resume resume,
                                  Job job,
                                  int compatibility,
                                  int totalRequirements,
                                  List<String> matched,
                                  List<String> missing,
                                  List<String> recommendations) {
        Analysis analysis = new Analysis(resume, job, compatibility, totalRequirements);
        analysis.matchedRequirements.addAll(matched);
        analysis.missingRequirements.addAll(missing);
        analysis.recommendations.addAll(recommendations);
        analysis.matchedCount = matched.size();
        return analysis;
    }

    public List<String> getMatchedRequirements() {
        return List.copyOf(matchedRequirements);
    }

    public List<String> getMissingRequirements() {
        return List.copyOf(missingRequirements);
    }

    public List<String> getRecommendations() {
        return List.copyOf(recommendations);
    }
}
