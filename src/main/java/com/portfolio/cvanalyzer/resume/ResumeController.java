package com.portfolio.cvanalyzer.resume;

import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import com.portfolio.cvanalyzer.common.dto.PageResponse;
import com.portfolio.cvanalyzer.resume.dto.ResumeResponse;
import com.portfolio.cvanalyzer.resume.dto.ResumeSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Curriculos", description = "Upload e consulta de curriculos")
public class ResumeController {

    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;

    private final ResumeService service;

    public ResumeController(ResumeService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia um curriculo em PDF e extrai seus dados")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Curriculo processado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Arquivo ausente, grande demais ou de tipo invalido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422", description = "PDF corrompido, protegido ou sem texto")
    })
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestPart("file")
            @Parameter(description = "Arquivo PDF do curriculo")
            MultipartFile file) {

        ResumeResponse criado = service.upload(file);

        // 201 + header Location apontando para o recurso criado.
        // E o que o padrao REST manda: o cliente descobre a URL do que
        // acabou de criar sem precisar montar a string na mao.
        URI location = UriComponentsBuilder.fromPath("/api/v1/resumes/{id}")
                .buildAndExpand(criado.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.ok(criado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um curriculo pelo identificador")
    public ResponseEntity<ApiResponse<ResumeResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @GetMapping
    @Operation(summary = "Lista curriculos, do mais recente para o mais antigo")
    public ResponseEntity<ApiResponse<PageResponse<ResumeSummaryResponse>>> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "" + TAMANHO_PAGINA_PADRAO) int tamanho) {

        // Limite maximo de pagina: sem isso, `?tamanho=1000000` derruba a
        // aplicacao carregando a tabela inteira em memoria.
        Pageable pageable = PageRequest.of(
                Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_PAGINA_MAXIMO));

        Page<ResumeSummaryResponse> page = service.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um curriculo e todo o seu historico de analises")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.delete(id);
        // 204: sucesso sem corpo. Devolver envelope aqui seria ruido.
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
