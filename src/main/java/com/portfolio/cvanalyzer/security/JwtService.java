package com.portfolio.cvanalyzer.security;

import com.portfolio.cvanalyzer.auth.AppUser;
import com.portfolio.cvanalyzer.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emissao e leitura do token JWT.
 *
 * O token guarda quem e o usuario (subject = id), o e-mail e o papel. Com
 * isso a API fica sem estado: nenhuma sessao em memoria, nenhuma consulta
 * ao banco para saber quem esta chamando — o que permite rodar varias
 * instancias atras de um balanceador sem sessao compartilhada.
 *
 * O preco dessa escolha: um token continua valido ate expirar, mesmo que o
 * papel do usuario mude no banco. Por isso a validade e curta (1 hora por
 * padrao) e o papel viaja dentro do token, nao fora dele.
 *
 * O Clock e injetado (bean da aplicacao) para o teste conseguir congelar o
 * tempo e verificar expiracao sem `Thread.sleep`.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(AppUser user) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(properties.expiration());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(expiraEm))
                .signWith(key)
                .compact();
    }

    public long expirationSeconds() {
        return properties.expiration().toSeconds();
    }

    /**
     * Le o token e devolve o usuario que ele representa.
     *
     * Devolve Optional vazio em vez de lancar excecao: token invalido,
     * expirado ou adulterado nao e erro de servidor, e uma requisicao
     * anonima — quem decide o que fazer com isso e a cadeia de seguranca,
     * que responde 401 pelo caminho normal.
     */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class))));

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token rejeitado: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
