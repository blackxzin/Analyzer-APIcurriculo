package com.portfolio.cvanalyzer.analysis.engine;

import java.util.List;

/**
 * Resultado do calculo de compatibilidade, antes de virar entidade ou DTO.
 */
public record CompatibilityScore(
        int percentage,
        List<String> matched,
        List<String> missing,
        int totalRequirements
) {
}
