package com.portfolio.cvanalyzer.resume;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
 * Curriculo processado.
 *
 * Sobre imutabilidade: DTOs neste projeto sao `record` e imutaveis. Entidade
 * JPA nao pode ser: o Hibernate precisa de construtor sem argumentos e de
 * capacidade de alterar campos para materializar o objeto vindo do banco.
 * O que fazemos e reduzir o dano — sem `@Setter` publico, construcao apenas
 * pelo metodo de fabrica `create`, e colecoes expostas como copia nao
 * modificavel.
 *
 * FetchType.LAZY em tudo: carregar as tecnologias e todas as secoes a cada
 * listagem de curriculos geraria consultas inuteis.
 */
@Entity
@Table(name = "resume")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "candidate_name", length = 255)
    private String candidateName;

    @Column(length = 320)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    private String rawText;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "resume_technology",
            joinColumns = @JoinColumn(name = "resume_id"))
    @Column(name = "technology", nullable = false, length = 80)
    private Set<String> technologies = new LinkedHashSet<>();

    @OneToMany(mappedBy = "resume",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<ResumeSectionItem> sectionItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Resume(String fileName, long fileSizeBytes, int pageCount, String rawText) {
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.pageCount = pageCount;
        this.rawText = rawText;
    }

    /**
     * Unico caminho para criar um curriculo. Metodo de fabrica com nome
     * proprio deixa a intencao explicita e concentra a validacao de
     * invariante num lugar so.
     */
    public static Resume create(String fileName, long fileSizeBytes, int pageCount, String rawText) {
        return new Resume(fileName, fileSizeBytes, pageCount, rawText);
    }

    public void applyExtractedData(String candidateName, String email, String phone) {
        this.candidateName = candidateName;
        this.email = email;
        this.phone = phone;
    }

    public void replaceTechnologies(Set<String> novasTecnologias) {
        this.technologies.clear();
        this.technologies.addAll(novasTecnologias);
    }

    /**
     * Substitui as linhas de uma secao.
     *
     * Metodo na propria entidade (e nao no service manipulando a lista de
     * fora) porque manter os dois lados da relacao em sincronia e regra da
     * entidade. Se o service esquecer de setar o `resume` no item, o
     * orphanRemoval apaga a linha sem aviso.
     */
    public void replaceSection(SectionType tipo, List<String> conteudos) {
        sectionItems.removeIf(item -> item.getSectionType() == tipo);
        int ordem = 0;
        for (String conteudo : conteudos) {
            sectionItems.add(new ResumeSectionItem(this, tipo, conteudo, ordem++));
        }
    }

    /** Copia nao modificavel: ninguem altera o estado interno por fora. */
    public Set<String> getTechnologies() {
        return Set.copyOf(technologies);
    }

    public List<String> itemsOf(SectionType tipo) {
        return sectionItems.stream()
                .filter(item -> item.getSectionType() == tipo)
                .map(ResumeSectionItem::getContent)
                .toList();
    }
}
