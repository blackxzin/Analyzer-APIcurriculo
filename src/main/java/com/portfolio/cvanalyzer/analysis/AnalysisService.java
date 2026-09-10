package com.portfolio.cvanalyzer.analysis;

import com.portfolio.cvanalyzer.analysis.dto.AnalysisRequest;
import com.portfolio.cvanalyzer.analysis.dto.AnalysisResponse;
import com.portfolio.cvanalyzer.analysis.dto.AnalysisSummaryResponse;
import com.portfolio.cvanalyzer.analysis.engine.CompatibilityScore;
import com.portfolio.cvanalyzer.analysis.engine.CompatibilityScorer;
import com.portfolio.cvanalyzer.analysis.engine.RecommendationEngine;
import com.portfolio.cvanalyzer.common.exception.ResourceNotFoundException;
import com.portfolio.cvanalyzer.job.Job;
import com.portfolio.cvanalyzer.job.JobService;
import com.portfolio.cvanalyzer.report.AnalysisReportGenerator;
import com.portfolio.cvanalyzer.resume.Resume;
import com.portfolio.cvanalyzer.resume.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Orquestra a analise: busca curriculo e vaga, calcula, gera recomendacoes
 * e grava o historico.
 *
 * Repare que o service NAO calcula nada. Ele coordena. O calculo esta em
 * CompatibilityScorer e o texto em RecommendationEngine, cada um testavel
 * sozinho. Service que calcula vira classe de mil linhas impossivel de testar.
 */
@Service
@Transactional(readOnly = true)
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisRepository repository;
    private final ResumeService resumeService;
    private final JobService jobService;
    private final CompatibilityScorer scorer;
    private final RecommendationEngine recommendationEngine;
    private final AnalysisMapper mapper;
    private final AnalysisReportGenerator reportGenerator;

    public AnalysisService(AnalysisRepository repository,
                           ResumeService resumeService,
                           JobService jobService,
                           CompatibilityScorer scorer,
                           RecommendationEngine recommendationEngine,
                           AnalysisMapper mapper,
                           AnalysisReportGenerator reportGenerator) {
        this.repository = repository;
        this.resumeService = resumeService;
        this.jobService = jobService;
        this.scorer = scorer;
        this.recommendationEngine = recommendationEngine;
        this.mapper = mapper;
        this.reportGenerator = reportGenerator;
    }

    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request) {
        Resume resume = resumeService.buscarOuFalhar(request.curriculoId());
        Job job = jobService.buscarOuFalhar(request.vagaId());

        CompatibilityScore score = scorer.score(resume, job);
        List<String> recomendacoes = recommendationEngine.generate(
                score.percentage(), score.missing(), score.matched());

        Analysis analysis = Analysis.create(
                resume, job,
                score.percentage(),
                score.totalRequirements(),
                score.matched(),
                score.missing(),
                recomendacoes);

        // saveAndFlush, e nao save: o @CreationTimestamp so e preenchido
        // quando o INSERT vai para o banco. Com save() puro o INSERT fica
        // agendado ate o commit — que acontece DEPOIS do mapper rodar — e o
        // campo `analisadoEm` sairia null na resposta.
        //
        // (Curriculo e vaga nao sofrem disso por acidente: as colecoes filhas
        // deles usam IDENTITY, o que obriga o Hibernate a dar flush antes.)
        Analysis salva = repository.saveAndFlush(analysis);
        log.info("Analise {} concluida: curriculo {} x vaga {} = {}%",
                salva.getId(), resume.getId(), job.getId(), score.percentage());

        return mapper.toResponse(salva);
    }

    public AnalysisResponse findById(UUID id) {
        Analysis analysis = repository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analise", id));
        return mapper.toResponse(analysis);
    }

    /**
     * Mesma analise da consulta por id, so que em PDF.
     *
     * Reaproveita findById de proposito: o relatorio nao pode divergir do
     * JSON, e havendo duas leituras diferentes um dia divergiriam.
     */
    public byte[] pdfReport(UUID id) {
        return reportGenerator.generate(findById(id));
    }

    public Page<AnalysisSummaryResponse> history(UUID curriculoId, UUID vagaId, Pageable pageable) {
        return repository.findHistory(curriculoId, vagaId, pageable).map(mapper::toSummary);
    }
}
