package com.portfolio.cvanalyzer.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuracao do token JWT, lida de `app.jwt`.
 *
 * O segredo vem de variavel de ambiente e NUNCA fica no codigo. O @Size
 * minimo de 32 caracteres nao e capricho: HS256 assina com HMAC-SHA-256 e
 * um segredo menor que o tamanho do bloco enfraquece a assinatura — a
 * propria biblioteca recusa a chave. Validando aqui, a aplicacao nao sobe
 * com configuracao fraca em vez de falhar so no primeiro login.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank(message = "defina a variavel de ambiente JWT_SECRET")
        @Size(min = 32, message = "o segredo do JWT deve ter ao menos 32 caracteres")
        String secret,

        Duration expiration,

        @NotBlank
        String issuer
) {

    public JwtProperties {
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            expiration = Duration.ofHours(1);
        }
    }
}
