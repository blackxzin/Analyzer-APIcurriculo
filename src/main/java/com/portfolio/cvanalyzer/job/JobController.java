package com.portfolio.cvanalyzer.job;

import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import com.portfolio.cvanalyzer.common.dto.PageResponse;
import com.portfolio.cvanalyzer.job.dto.JobRequest;
import com.portfolio.cvanalyzer.job.dto.JobResponse;
import com.portfolio.cvanalyzer.job.dto.JobSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Vagas", description = "Cadastro e consulta de vagas")
public class JobController {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastra uma vaga")
    public ResponseEntity<ApiResponse<JobResponse>> criar(@Valid @RequestBody JobRequest request) {
        JobResponse criada = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/jobs/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.ok(criada));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma vaga")
    public ResponseEntity<ApiResponse<JobResponse>> atualizar(@PathVariable UUID id,
                                                              @Valid @RequestBody JobRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma vaga pelo identificador")
    public ResponseEntity<ApiResponse<JobResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @GetMapping
    @Operation(summary = "Lista vagas, da mais recente para a mais antiga")
    public ResponseEntity<ApiResponse<PageResponse<JobSummaryResponse>>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + TAMANHO_PAGINA_PADRAO) int tamanho) {

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_PAGINA_MAXIMO));

        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(service.findAll(pageable))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma vaga e todo o seu historico de analises")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
