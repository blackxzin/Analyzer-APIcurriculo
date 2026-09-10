package com.portfolio.cvanalyzer.security;

import com.portfolio.cvanalyzer.auth.Role;

import java.util.UUID;

/**
 * Quem esta do outro lado da requisicao, montado a partir do token.
 *
 * E um record pequeno e imutavel, e nao a entidade AppUser, por dois motivos:
 * evita uma consulta ao banco a cada requisicao (o token ja carrega o que
 * precisamos) e mantem o hash da senha longe do contexto de seguranca.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {
}
