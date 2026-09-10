package com.portfolio.cvanalyzer.resume;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Uma linha de uma secao do curriculo (uma formacao, uma experiencia, etc).
 *
 * @Enumerated(STRING) e obrigatorio aqui. O padrao do JPA e ORDINAL, que
 * grava o indice do enum (0, 1, 2...). Se alguem reordenar o enum depois,
 * todos os dados ja gravados passam a significar outra coisa — bug silencioso
 * e irreversivel. STRING grava "EDUCATION" e resiste a reordenacao.
 */
@Entity
@Table(name = "resume_section_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeSectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 30)
    private SectionType sectionType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    ResumeSectionItem(Resume resume, SectionType sectionType, String content, int sortOrder) {
        this.resume = resume;
        this.sectionType = sectionType;
        this.content = content;
        this.sortOrder = sortOrder;
    }
}
