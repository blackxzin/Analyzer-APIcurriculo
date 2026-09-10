package com.portfolio.cvanalyzer.job;

import com.portfolio.cvanalyzer.job.dto.JobRequest;
import com.portfolio.cvanalyzer.technology.TechnologyCatalog;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Decide qual e a lista final de requisitos de uma vaga.
 *
 * Duas fontes possiveis:
 *  1. o usuario informou os requisitos -> usamos os dele, canonicalizados;
 *  2. o usuario nao informou nada       -> extraimos da descricao.
 *
 * A canonicalizacao e o ponto critico. Sem ela, a vaga pediria "springboot"
 * e o curriculo ofereceria "Spring Boot": strings diferentes, match zero,
 * nota errada. Passar pelo mesmo catalogo dos dois lados garante que a
 * comparacao seja justa.
 */
@Component
public class JobRequirementResolver {

    private final TechnologyCatalog catalog;

    public JobRequirementResolver(TechnologyCatalog catalog) {
        this.catalog = catalog;
    }

    public record ResolvedRequirements(List<String> mandatory, List<String> optional) {

        public boolean isEmpty() {
            return mandatory.isEmpty() && optional.isEmpty();
        }
    }

    public ResolvedRequirements resolve(JobRequest request) {
        List<String> informadosObrigatorios = canonicalizar(request.requisitosObrigatorios());
        List<String> desejaveis = canonicalizar(request.requisitosDesejaveis());

        // Nenhum requisito informado: extraimos da descricao e tratamos
        // todos como obrigatorios, que e a leitura conservadora.
        final List<String> obrigatorios =
                informadosObrigatorios.isEmpty() && desejaveis.isEmpty()
                        ? List.copyOf(catalog.extractFrom(request.descricao()))
                        : informadosObrigatorios;

        // Remove dos desejaveis o que ja e obrigatorio.
        List<String> desejaveisFiltrados = desejaveis.stream()
                .filter(requisito -> !obrigatorios.contains(requisito))
                .toList();

        return new ResolvedRequirements(obrigatorios, desejaveisFiltrados);
    }

    private List<String> canonicalizar(List<String> termos) {
        Set<String> resultado = new LinkedHashSet<>();
        for (String termo : termos) {
            if (termo == null || termo.isBlank()) {
                continue;
            }
            String canonico = catalog.canonicalize(termo);
            if (!canonico.isBlank()) {
                resultado.add(canonico);
            }
        }
        return List.copyOf(resultado);
    }
}
