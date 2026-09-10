package com.portfolio.cvanalyzer.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso da API.
 *
 * Decisoes e o porque de cada uma:
 *
 * - STATELESS: nenhuma sessao no servidor. Toda requisicao se identifica
 *   pelo token. E o que permite escalar em varias instancias sem sessao
 *   compartilhada.
 *
 * - CSRF desligado: CSRF explora o envio AUTOMATICO de credencial pelo
 *   navegador (cookie). Aqui a credencial vai num header que o cliente
 *   precisa montar de proposito, entao o ataque nao se aplica. Se um dia
 *   o token passar a viajar em cookie, o CSRF volta a ser obrigatorio.
 *
 * - Lista de rotas publicas curta e explicita, e `anyRequest().authenticated()`
 *   no fim: o padrao passa a ser NEGAR. Uma rota nova nasce protegida, e
 *   esquecer de listar nao abre buraco — no maximo quebra o teste.
 *
 * - DELETE so para ADMIN: remover curriculo ou vaga apaga o historico de
 *   analises em cascata. E a unica operacao irreversivel da API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/health",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   ApiAuthenticationEntryPoint entryPoint,
                                                   ApiAccessDeniedHandler accessDeniedHandler)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/resumes/**", "/api/v1/jobs/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * BCrypt: algoritmo de hash proprio para senha, lento de proposito.
     *
     * Hash rapido (MD5, SHA-256) e problema aqui: uma GPU testa bilhoes de
     * candidatos por segundo. BCrypt tem custo configuravel e embute um
     * salt aleatorio em cada hash, entao duas pessoas com a mesma senha
     * geram hashes diferentes e uma rainbow table nao serve para nada.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
