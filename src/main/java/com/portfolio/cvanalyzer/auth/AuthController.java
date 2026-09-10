package com.portfolio.cvanalyzer.auth;

import com.portfolio.cvanalyzer.auth.dto.AuthResponse;
import com.portfolio.cvanalyzer.auth.dto.LoginRequest;
import com.portfolio.cvanalyzer.auth.dto.RegisterRequest;
import com.portfolio.cvanalyzer.auth.dto.UserResponse;
import com.portfolio.cvanalyzer.common.dto.ApiResponse;
import com.portfolio.cvanalyzer.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacao", description = "Cadastro, login e dados do usuario autenticado")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastra um usuario e ja devolve o token de acesso")
    public ResponseEntity<ApiResponse<AuthResponse>> cadastrar(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token de acesso")
    public ResponseEntity<ApiResponse<AuthResponse>> entrar(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.login(request)));
    }

    /**
     * @AuthenticationPrincipal entrega o objeto que o filtro JWT colocou no
     * contexto de seguranca. Nao existe consulta de "quem sou eu" por id vindo
     * do cliente — o id vem do token assinado, entao ninguem consulta os dados
     * de outra pessoa trocando um numero na URL.
     */
    @GetMapping("/me")
    @Operation(summary = "Dados do usuario dono do token enviado")
    public ResponseEntity<ApiResponse<UserResponse>> eu(@AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(ApiResponse.ok(service.current(usuario.id())));
    }
}
