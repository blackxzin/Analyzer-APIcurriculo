-- =====================================================================
-- Analises (historico de comparacoes curriculo x vaga)
-- =====================================================================
CREATE TABLE analysis (
    id                 UUID        PRIMARY KEY,
    resume_id          UUID        NOT NULL REFERENCES resume (id) ON DELETE CASCADE,
    job_id             UUID        NOT NULL REFERENCES job (id)    ON DELETE CASCADE,
    compatibility      INTEGER     NOT NULL,
    matched_count      INTEGER     NOT NULL,
    total_requirements INTEGER     NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_analysis_compatibility CHECK (compatibility BETWEEN 0 AND 100)
);

-- O historico e quase sempre consultado por curriculo ou por vaga,
-- do mais recente para o mais antigo.
CREATE INDEX idx_analysis_resume  ON analysis (resume_id, created_at DESC);
CREATE INDEX idx_analysis_job     ON analysis (job_id, created_at DESC);
CREATE INDEX idx_analysis_created ON analysis (created_at DESC);

-- Resultado congelado no momento da analise.
-- Guardamos o resultado em vez de recalcular: se o curriculo ou a vaga
-- mudarem depois, o historico continua contando a verdade daquele dia.
CREATE TABLE analysis_matched_requirement (
    analysis_id UUID        NOT NULL REFERENCES analysis (id) ON DELETE CASCADE,
    requirement VARCHAR(80) NOT NULL,
    PRIMARY KEY (analysis_id, requirement)
);

CREATE TABLE analysis_missing_requirement (
    analysis_id UUID        NOT NULL REFERENCES analysis (id) ON DELETE CASCADE,
    requirement VARCHAR(80) NOT NULL,
    PRIMARY KEY (analysis_id, requirement)
);

CREATE TABLE analysis_recommendation (
    id             BIGSERIAL PRIMARY KEY,
    analysis_id    UUID      NOT NULL REFERENCES analysis (id) ON DELETE CASCADE,
    recommendation TEXT      NOT NULL,
    sort_order       INTEGER   NOT NULL
);

CREATE INDEX idx_analysis_recommendation ON analysis_recommendation (analysis_id, sort_order);
