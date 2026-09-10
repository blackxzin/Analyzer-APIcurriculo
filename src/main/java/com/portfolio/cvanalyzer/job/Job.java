package com.portfolio.cvanalyzer.job;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Vaga de emprego com seus requisitos.
 */
@Entity
@Table(name = "job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 150)
    private String company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Seniority seniority;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "job",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<JobRequirement> requirements = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Job(String title, String company, Seniority seniority, String description) {
        this.title = title;
        this.company = company;
        this.seniority = seniority;
        this.description = description;
    }

    public static Job create(String title, String company, Seniority seniority, String description) {
        return new Job(title, company, seniority, description);
    }

    public void update(String title, String company, Seniority seniority, String description) {
        this.title = title;
        this.company = company;
        this.seniority = seniority;
        this.description = description;
    }

    /**
     * Troca a lista inteira de requisitos.
     *
     * ATENCAO ao usar isto em uma vaga JA PERSISTIDA: o Hibernate executa
     * todos os INSERT antes dos DELETE dentro da mesma transacao, entao
     * remover "Java" e inserir "Java" de novo estoura a constraint
     * UNIQUE (job_id, requirement). Nesse caso use clearRequirements(),
     * force um flush e so entao chame addRequirements() — e o que o
     * JobService.update faz.
     */
    public void replaceRequirements(List<String> obrigatorios, List<String> desejaveis) {
        clearRequirements();
        addRequirements(obrigatorios, desejaveis);
    }

    public void clearRequirements() {
        requirements.clear();
    }

    /**
     * Um Set intermediario evita requisito duplicado — a tabela tem
     * UNIQUE (job_id, requirement) e um duplicado estouraria a constraint
     * so na hora do commit, com mensagem feia de banco.
     *
     * Obrigatorio ganha do desejavel: se o mesmo termo aparecer nas duas
     * listas, ele fica como obrigatorio.
     */
    public void addRequirements(List<String> obrigatorios, List<String> desejaveis) {
        Set<String> jaAdicionados = new LinkedHashSet<>();
        for (String requisito : obrigatorios) {
            if (jaAdicionados.add(requisito)) {
                requirements.add(new JobRequirement(this, requisito, true));
            }
        }
        for (String requisito : desejaveis) {
            if (jaAdicionados.add(requisito)) {
                requirements.add(new JobRequirement(this, requisito, false));
            }
        }
    }

    public List<String> mandatoryRequirements() {
        return requirements.stream()
                .filter(JobRequirement::isMandatory)
                .map(JobRequirement::getRequirement)
                .toList();
    }

    public List<String> optionalRequirements() {
        return requirements.stream()
                .filter(requisito -> !requisito.isMandatory())
                .map(JobRequirement::getRequirement)
                .toList();
    }
}
