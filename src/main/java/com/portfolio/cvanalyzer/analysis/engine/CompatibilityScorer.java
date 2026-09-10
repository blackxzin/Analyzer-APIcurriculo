package com.portfolio.cvanalyzer.analysis.engine;

import com.portfolio.cvanalyzer.job.Job;
import com.portfolio.cvanalyzer.resume.Resume;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Calcula a nota de compatibilidade entre um curriculo e uma vaga.
 *
 * REGRA DE PONTUACAO
 * ------------------
 * Cada requisito vale um peso:
 *   - obrigatorio ......... peso 3
 *   - desejavel ........... peso 1
 *
 * nota = 100 * (soma dos pesos atendidos) / (soma de todos os pesos)
 *
 * Por que peso e nao contagem simples? Porque contar igual distorce a
 * realidade: um candidato que domina 5 diferenciais mas nao sabe o requisito
 * obrigatorio da vaga nao esta 83% compativel — esta longe. O peso 3x faz
 * o obrigatorio dominar a nota, como acontece numa triagem de verdade.
 *
 * A classe nao acessa banco e nao conhece HTTP: entra curriculo e vaga,
 * sai numero. Isso a torna testavel isoladamente, e a regra de negocio
 * mais importante do sistema merece exatamente esse tratamento.
 */
@Component
public class CompatibilityScorer {

    static final int PESO_OBRIGATORIO = 3;
    static final int PESO_DESEJAVEL = 1;

    private static final int NOTA_MINIMA = 0;
    private static final int NOTA_MAXIMA = 100;

    public CompatibilityScore score(Resume resume, Job job) {
        Set<String> tecnologiasDoCandidato = resume.getTechnologies();

        List<String> obrigatorios = job.mandatoryRequirements();
        List<String> desejaveis = job.optionalRequirements();

        List<String> atendidos = new ArrayList<>();
        List<String> ausentes = new ArrayList<>();

        int pesoTotal = 0;
        int pesoAtendido = 0;

        for (String requisito : obrigatorios) {
            pesoTotal += PESO_OBRIGATORIO;
            if (tecnologiasDoCandidato.contains(requisito)) {
                pesoAtendido += PESO_OBRIGATORIO;
                atendidos.add(requisito);
            } else {
                ausentes.add(requisito);
            }
        }

        for (String requisito : desejaveis) {
            pesoTotal += PESO_DESEJAVEL;
            if (tecnologiasDoCandidato.contains(requisito)) {
                pesoAtendido += PESO_DESEJAVEL;
                atendidos.add(requisito);
            } else {
                ausentes.add(requisito);
            }
        }

        int nota = calcularPercentual(pesoAtendido, pesoTotal);
        return new CompatibilityScore(nota, List.copyOf(atendidos), List.copyOf(ausentes),
                obrigatorios.size() + desejaveis.size());
    }

    private int calcularPercentual(int pesoAtendido, int pesoTotal) {
        if (pesoTotal == 0) {
            // Vaga sem requisitos nao deveria existir (JobService barra isso),
            // mas dividir por zero derrubaria a aplicacao. Guarda de seguranca.
            return NOTA_MINIMA;
        }
        // Math.round em double: arredonda 78.5 para 79, e nao trunca para 78
        // como faria a divisao inteira.
        long nota = Math.round((double) pesoAtendido * NOTA_MAXIMA / pesoTotal);
        return (int) Math.clamp(nota, NOTA_MINIMA, NOTA_MAXIMA);
    }
}
