package com.portfolio.cvanalyzer.resume;

/**
 * Secoes reconhecidas dentro de um curriculo.
 *
 * Enum e nao String solta: o compilador impede um valor invalido e o
 * conjunto de secoes fica documentado em um lugar so.
 */
public enum SectionType {
    EDUCATION,
    EXPERIENCE,
    COURSE,
    CERTIFICATION,
    LANGUAGE
}
