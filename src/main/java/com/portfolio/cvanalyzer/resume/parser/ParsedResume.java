package com.portfolio.cvanalyzer.resume.parser;

import com.portfolio.cvanalyzer.resume.SectionType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tudo que conseguimos entender de um curriculo, antes de virar entidade.
 *
 * Objeto intermediario de proposito: separa "interpretar o texto" de
 * "gravar no banco". Da para testar o parser inteiro sem tocar em JPA.
 */
public record ParsedResume(
        String candidateName,
        String email,
        String phone,
        Set<String> technologies,
        Map<SectionType, List<String>> sections
) {
    public List<String> section(SectionType tipo) {
        return sections.getOrDefault(tipo, List.of());
    }
}
