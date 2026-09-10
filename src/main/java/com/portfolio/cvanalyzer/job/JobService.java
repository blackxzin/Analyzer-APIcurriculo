package com.portfolio.cvanalyzer.job;

import com.portfolio.cvanalyzer.common.exception.ResourceNotFoundException;
import com.portfolio.cvanalyzer.job.dto.JobRequest;
import com.portfolio.cvanalyzer.job.dto.JobResponse;
import com.portfolio.cvanalyzer.job.dto.JobSummaryResponse;
import com.portfolio.cvanalyzer.job.exception.JobWithoutRequirementsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository repository;
    private final JobRequirementResolver requirementResolver;
    private final JobMapper mapper;

    public JobService(JobRepository repository,
                      JobRequirementResolver requirementResolver,
                      JobMapper mapper) {
        this.repository = repository;
        this.requirementResolver = requirementResolver;
        this.mapper = mapper;
    }

    @Transactional
    public JobResponse create(JobRequest request) {
        var requisitos = requirementResolver.resolve(request);
        if (requisitos.isEmpty()) {
            throw new JobWithoutRequirementsException();
        }

        Job job = Job.create(
                request.titulo().strip(),
                request.empresa().strip(),
                request.senioridade(),
                request.descricao().strip());
        job.replaceRequirements(requisitos.mandatory(), requisitos.optional());

        Job salva = repository.save(job);
        log.info("Vaga {} criada com {} requisitos", salva.getId(), salva.getRequirements().size());
        return mapper.toResponse(salva);
    }

    @Transactional
    public JobResponse update(UUID id, JobRequest request) {
        Job job = buscarOuFalhar(id);

        var requisitos = requirementResolver.resolve(request);
        if (requisitos.isEmpty()) {
            throw new JobWithoutRequirementsException();
        }

        job.update(
                request.titulo().strip(),
                request.empresa().strip(),
                request.senioridade(),
                request.descricao().strip());

        // Os requisitos precisam de tres passos, nesta ordem:
        //
        //   1. limpar a colecao
        //   2. FORCAR o flush (executa os DELETE agora)
        //   3. inserir os novos
        //
        // Sem o passo 2 o Hibernate agenda todos os INSERT antes dos DELETE,
        // e atualizar uma vaga mantendo "Java" na lista estoura a constraint
        // UNIQUE (job_id, requirement) com erro 500. Bug classico de JPA.
        job.clearRequirements();
        repository.flush();
        job.addRequirements(requisitos.mandatory(), requisitos.optional());

        // Sem repository.save(): dentro da transacao a entidade esta
        // gerenciada e o Hibernate detecta a mudanca sozinho no commit
        // (dirty checking). Chamar save aqui seria redundante.
        log.info("Vaga {} atualizada", id);
        return mapper.toResponse(job);
    }

    public JobResponse findById(UUID id) {
        return mapper.toResponse(buscarOuFalhar(id));
    }

    public Page<JobSummaryResponse> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(mapper::toSummary);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(buscarOuFalhar(id));
        log.info("Vaga {} removida", id);
    }

    public Job buscarOuFalhar(UUID id) {
        return repository.findWithRequirementsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga", id));
    }
}
