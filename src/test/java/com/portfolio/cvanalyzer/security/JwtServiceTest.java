package com.portfolio.cvanalyzer.security;

import com.portfolio.cvanalyzer.auth.AppUser;
import com.portfolio.cvanalyzer.auth.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do token, sem Spring e sem banco.
 *
 * O relogio e fixo: expiracao e testada avancando o relogio, nunca com
 * Thread.sleep. Teste que dorme e teste lento e intermitente.
 */
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final Instant AGORA = Instant.parse("2026-01-15T10:00:00Z");
    private static final UUID ID_USUARIO = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final JwtProperties properties =
            new JwtProperties(SEGREDO, Duration.ofHours(1), "cv-analyzer-api");

    private JwtService servicoEm(Instant instante) {
        return new JwtService(properties, Clock.fixed(instante, ZoneOffset.UTC));
    }

    private AppUser usuario() {
        AppUser user = AppUser.create("Ana Souza", "ana@empresa.com", "hash-irrelevante", Role.ADMIN);
        // O id normalmente e gerado pelo banco; aqui fixamos para poder afirmar sobre ele.
        ReflectionTestUtils.setField(user, "id", ID_USUARIO);
        return user;
    }

    @Test
    @DisplayName("gera um token que devolve id, e-mail e papel ao ser lido")
    void geraELeOToken() {
        // Arrange
        JwtService service = servicoEm(AGORA);

        // Act
        String token = service.generate(usuario());
        Optional<AuthenticatedUser> lido = service.parse(token);

        // Assert
        assertThat(lido).isPresent();
        assertThat(lido.get().id()).isEqualTo(ID_USUARIO);
        assertThat(lido.get().email()).isEqualTo("ana@empresa.com");
        assertThat(lido.get().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("rejeita token expirado")
    void rejeitaTokenExpirado() {
        String token = servicoEm(AGORA).generate(usuario());

        JwtService duasHorasDepois = servicoEm(AGORA.plus(Duration.ofHours(2)));

        assertThat(duasHorasDepois.parse(token)).isEmpty();
    }

    @Test
    @DisplayName("rejeita token assinado com outro segredo")
    void rejeitaTokenDeOutroSegredo() {
        JwtProperties outroSegredo = new JwtProperties(
                "outro-segredo-completamente-diferente-e-longo", Duration.ofHours(1), "cv-analyzer-api");
        String token = new JwtService(outroSegredo, Clock.fixed(AGORA, ZoneOffset.UTC)).generate(usuario());

        assertThat(servicoEm(AGORA).parse(token)).isEmpty();
    }

    @Test
    @DisplayName("rejeita token adulterado")
    void rejeitaTokenAdulterado() {
        String token = servicoEm(AGORA).generate(usuario());
        String adulterado = token.substring(0, token.length() - 3) + "abc";

        assertThat(servicoEm(AGORA).parse(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("rejeita texto que nem token e")
    void rejeitaLixo() {
        assertThat(servicoEm(AGORA).parse("nao-e-um-token")).isEmpty();
    }

    @Test
    @DisplayName("expiracao ausente ou negativa cai no padrao de uma hora")
    void expiracaoPadrao() {
        JwtProperties semExpiracao = new JwtProperties(SEGREDO, null, "cv-analyzer-api");

        assertThat(new JwtService(semExpiracao, Clock.fixed(AGORA, ZoneOffset.UTC)).expirationSeconds())
                .isEqualTo(Duration.ofHours(1).toSeconds());
    }
}
