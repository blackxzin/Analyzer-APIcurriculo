package com.portfolio.cvanalyzer.health;

import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import com.portfolio.cvanalyzer.health.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Camada de entrada HTTP.
 *
 * Responsabilidade do controller, e so isso:
 *  1. receber a requisicao
 *  2. delegar ao service
 *  3. traduzir o retorno em status HTTP + corpo
 *
 * Zero regra de negocio, zero acesso a banco.
 *
 * @RestController = @Controller + @ResponseBody (retorno vira JSON direto).
 * Prefixo /api/v1 versiona a API: quando o contrato mudar de forma
 * incompativel, nasce /api/v2 e os clientes antigos continuam funcionando.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Verificacao de disponibilidade da API")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    @Operation(summary = "Verifica se a API esta no ar")
    public ResponseEntity<ApiResponse<HealthResponse>> health() {
        return ResponseEntity.ok(ApiResponse.ok(healthService.check()));
    }
}
