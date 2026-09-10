package com.portfolio.cvanalyzer.auth;

import com.portfolio.cvanalyzer.auth.dto.AuthResponse;
import com.portfolio.cvanalyzer.auth.dto.LoginRequest;
import com.portfolio.cvanalyzer.auth.dto.RegisterRequest;
import com.portfolio.cvanalyzer.auth.dto.UserResponse;
import com.portfolio.cvanalyzer.auth.exception.EmailAlreadyUsedException;
import com.portfolio.cvanalyzer.auth.exception.InvalidCredentialsException;
import com.portfolio.cvanalyzer.common.exception.ResourceNotFoundException;
import com.portfolio.cvanalyzer.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cadastro, login e consulta do usuario autenticado.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Hash descartavel usado quando o e-mail nao existe.
     *
     * Sem ele, um login com e-mail inexistente responderia na hora e um com
     * e-mail valido demoraria os ~100ms do BCrypt. Essa diferenca de tempo
     * e mensuravel e revela quais e-mails estao cadastrados. Comparar contra
     * um hash falso iguala o custo dos dois caminhos.
     */
    private static final String HASH_FALSO =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppUserMapper mapper;

    public AuthService(AppUserRepository repository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AppUserMapper mapper) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }

    /**
     * Cadastra e ja devolve o token — o usuario nao precisa fazer login logo
     * depois de se cadastrar.
     *
     * O PRIMEIRO usuario cadastrado vira ADMIN; os demais nascem RECRUTADOR.
     * Motivo: sem isso o sistema subiria vazio, sem ninguem capaz de executar
     * as operacoes de administrador, e a saida seria um usuario com senha fixa
     * no codigo ou numa migration — exatamente o tipo de credencial que vaza.
     * Promover outra pessoa a ADMIN e operacao de banco, feita de proposito
     * fora da API.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizar(request.email());

        if (repository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        Role papel = repository.existsByRole(Role.ADMIN) ? Role.RECRUTADOR : Role.ADMIN;

        AppUser usuario = AppUser.create(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()),
                papel);

        AppUser salvo = salvarTratandoCorrida(usuario, email);
        log.info("Usuario {} cadastrado como {}", salvo.getId(), salvo.getRole());

        return montarResposta(salvo);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizar(request.email());

        AppUser usuario = repository.findByEmailIgnoreCase(email).orElse(null);
        String hash = usuario != null ? usuario.getPasswordHash() : HASH_FALSO;

        if (!passwordEncoder.matches(request.senha(), hash) || usuario == null) {
            log.warn("Tentativa de login malsucedida para {}", email);
            throw new InvalidCredentialsException();
        }

        return montarResposta(usuario);
    }

    public UserResponse current(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    /**
     * A verificacao de e-mail duplicado acima resolve o caso comum e devolve
     * uma mensagem boa. Duas requisicoes simultaneas, porem, passam as duas
     * pela verificacao antes de qualquer INSERT — quem barra e o indice
     * unico do banco. Traduzimos o erro do banco para o mesmo 409, em vez de
     * deixar virar um 500 generico.
     */
    private AppUser salvarTratandoCorrida(AppUser usuario, String email) {
        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyUsedException(email);
        }
    }

    private AuthResponse montarResposta(AppUser usuario) {
        return AuthResponse.of(
                jwtService.generate(usuario),
                jwtService.expirationSeconds(),
                mapper.toResponse(usuario));
    }

    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
