-- =====================================================================
-- Vagas
-- =====================================================================
CREATE TABLE job (
    id          UUID         PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    company     VARCHAR(150) NOT NULL,
    seniority   VARCHAR(30)  NOT NULL,
    description TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_job_created_at ON job (created_at DESC);

-- ---------------------------------------------------------------------
-- Requisitos da vaga.
-- `mandatory` separa obrigatorio de desejavel: os dois entram na nota,
-- mas com pesos diferentes.
-- ---------------------------------------------------------------------
CREATE TABLE job_requirement (
    id          BIGSERIAL    PRIMARY KEY,
    job_id      UUID         NOT NULL REFERENCES job (id) ON DELETE CASCADE,
    requirement VARCHAR(80)  NOT NULL,
    mandatory   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_job_requirement UNIQUE (job_id, requirement)
);

CREATE INDEX idx_job_requirement_requirement ON job_requirement (requirement);
