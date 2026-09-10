-- =====================================================================
-- Usuarios da API
--
-- A senha NUNCA e guardada em texto. A coluna se chama `password_hash`
-- de proposito: o nome documenta o conteudo e impede que alguem, um dia,
-- ache que da para gravar a senha crua ali.
-- 60 caracteres e o tamanho fixo de um hash BCrypt; deixamos 100 de folga
-- para uma eventual troca de algoritmo (Argon2 gera hash maior).
-- =====================================================================
CREATE TABLE app_user (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Unicidade sobre LOWER(email): "Ana@x.com" e "ana@x.com" sao a mesma pessoa.
-- Fazer isso so na aplicacao nao basta — duas requisicoes simultaneas passariam
-- as duas pela verificacao antes de qualquer INSERT. O banco e a ultima palavra.
CREATE UNIQUE INDEX uk_app_user_email ON app_user (LOWER(email));
