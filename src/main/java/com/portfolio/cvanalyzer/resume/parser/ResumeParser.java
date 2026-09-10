package com.portfolio.cvanalyzer.resume.parser;

import com.portfolio.cvanalyzer.resume.SectionType;
import com.portfolio.cvanalyzer.technology.TechnologyCatalog;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orquestra os extratores especializados.
 *
 * Cada extrator sabe achar UMA coisa (SRP). Esta classe apenas os coordena
 * e monta o resultado. Adicionar "extrair pretensao salarial" amanha e
 * criar mais um extrator e uma linha aqui — nenhum codigo existente muda
 * (aberto para extensao, fechado para modificacao).
 */
@Component
public class ResumeParser {

    private final NameExtractor nameExtractor;
    private final EmailExtractor emailExtractor;
    private final PhoneExtractor phoneExtractor;
    private final SectionExtractor sectionExtractor;
    private final TechnologyCatalog technologyCatalog;

    public ResumeParser(NameExtractor nameExtractor,
                        EmailExtractor emailExtractor,
                        PhoneExtractor phoneExtractor,
                        SectionExtractor sectionExtractor,
                        TechnologyCatalog technologyCatalog) {
        this.nameExtractor = nameExtractor;
        this.emailExtractor = emailExtractor;
        this.phoneExtractor = phoneExtractor;
        this.sectionExtractor = sectionExtractor;
        this.technologyCatalog = technologyCatalog;
    }

    public ParsedResume parse(String texto) {
        String nome = nameExtractor.extract(texto).orElse(null);
        String email = emailExtractor.extract(texto).orElse(null);
        String telefone = phoneExtractor.extract(texto).orElse(null);

        Set<String> tecnologias = technologyCatalog.extractFrom(texto);
        Map<SectionType, List<String>> secoes = sectionExtractor.extract(texto);

        return new ParsedResume(nome, email, telefone, tecnologias, secoes);
    }
}
