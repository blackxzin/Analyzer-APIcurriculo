package com.portfolio.cvanalyzer.analysis;

import com.portfolio.cvanalyzer.analysis.dto.AnalysisRequest;
import com.portfolio.cvanalyzer.analysis.dto.AnalysisResponse;
import com.portfolio.cvanalyzer.analysis.dto.AnalysisSummaryResponse;
import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import com.portfolio.cvanalyzer.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analyses")
@Tag(name = "Analises", description = "Comparacao entre curriculo e vaga, e historico")
public class AnalysisController {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Compara um curriculo com uma vaga e gera a pontuacao")
    public ResponseEntity<ApiResponse<AnalysisResponse>> analisar(
            @Valid @RequestBody AnalysisRequest request) {

        AnalysisResponse resultado = service.analyze(request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/analyses/{id}")
                .buildAndExpand(resultado.id())
                .toUri();
        return ResponseEntity.created(location).body(ApiResponse.ok(resultado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma analise pelo identificador")
    public ResponseEntity<ApiResponse<AnalysisResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    /**
     * Devolve bytes, e nao o envelope ApiResponse: o corpo aqui e o proprio
     * arquivo. O header Content-Disposition com `attachment` faz o navegador
     * baixar em vez de tentar exibir, e ja sugere o nome do arquivo.
     */
    @GetMapping(value = "/{id}/relatorio", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Baixa o relatorio da analise em PDF")
    public ResponseEntity<byte[]> relatorio(@PathVariable UUID id) {
        byte[] pdf = service.pdfReport(id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("analise-%s.pdf".formatted(id))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping
    @Operation(summary = "Historico de analises, com filtro opcional por curriculo e/ou vaga")
    public ResponseEntity<ApiResponse<PageResponse<AnalysisSummaryResponse>>> historico(
            @RequestParam(required = false) UUID curriculoId,
            @RequestParam(required = false) UUID vagaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + TAMANHO_PAGINA_PADRAO) int tamanho) {

        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_PAGINA_MAXIMO));

        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(service.history(curriculoId, vagaId, pageable))));
    }
}
