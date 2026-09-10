package com.portfolio.cvanalyzer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Le o header `Authorization: Bearer <token>` e, se o token for valido,
 * marca a requisicao como autenticada.
 *
 * Estende OncePerRequestFilter porque um `forward` interno do servlet
 * container faria um filtro comum rodar duas vezes na mesma requisicao.
 *
 * O filtro NUNCA responde 401 por conta propria: se nao ha token, ou ele e
 * invalido, a requisicao apenas segue anonima. Quem decide se aquilo era
 * permitido ou nao e a regra de autorizacao la no SecurityConfig — separar
 * "quem e voce" de "voce pode" mantem cada peca com um trabalho so.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        extrairToken(request)
                .flatMap(jwtService::parse)
                .ifPresent(usuario -> autenticar(usuario, request));

        filterChain.doFilter(request, response);
    }

    private Optional<String> extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIXO_BEARER)) {
            return Optional.empty();
        }
        String token = header.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private void autenticar(AuthenticatedUser usuario, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                usuario,
                null,   // credenciais ja foram verificadas na assinatura do token
                List.of(new SimpleGrantedAuthority(usuario.role().authority())));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(authentication);
        SecurityContextHolder.setContext(contexto);
    }
}
