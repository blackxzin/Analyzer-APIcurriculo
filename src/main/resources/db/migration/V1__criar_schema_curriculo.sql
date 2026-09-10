-- =====================================================================
-- Curriculos
-- =====================================================================
CREATE TABLE resume (
    id              UUID         PRIMARY KEY,
    candidate_name  VARCHAR(255),
    email           VARCHAR(320),
    phone           VARCHAR(40),
    file_name       VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT       NOT NULL,
    page_count      INTEGER      NOT NULL,
    raw_text        TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Busca por e-mail e a forma natural de reencontrar um candidato.
CREATE INDEX idx_resume_email ON resume (email);
CREATE INDEX idx_resume_created_at ON resume (created_at DESC);

-- ---------------------------------------------------------------------
-- Tecnologias do curriculo.
-- Tabela propria (e nao coluna com lista separada por virgula) porque
-- assim da para indexar, contar e cruzar com os requisitos da vaga em SQL.
-- ---------------------------------------------------------------------
CREATE TABLE resume_technology (
    resume_id  UUID        NOT NULL REFERENCES resume (id) ON DELETE CASCADE,
    technology VARCHAR(80) NOT NULL,
    PRIMARY KEY (resume_id, technology)
);

CREATE INDEX idx_resume_technology_technology ON resume_technology (technology);

-- ---------------------------------------------------------------------
-- Demais secoes do curriculo (formacao, experiencia, cursos,
-- certificacoes, idiomas).
-- Uma tabela unica com discriminador em vez de cinco tabelas identicas:
-- mesmo formato, mesma consulta, menos codigo para manter (DRY).
-- ---------------------------------------------------------------------
CREATE TABLE resume_section_item (
    id           BIGSERIAL   PRIMARY KEY,
    resume_id    UUID        NOT NULL REFERENCES resume (id) ON DELETE CASCADE,
    section_type VARCHAR(30) NOT NULL,
    content      TEXT        NOT NULL,
    sort_order     INTEGER     NOT NULL
);

CREATE INDEX idx_resume_section_item_resume ON resume_section_item (resume_id, section_type, sort_order);
