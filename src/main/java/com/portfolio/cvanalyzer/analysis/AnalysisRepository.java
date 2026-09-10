package com.portfolio.cvanalyzer.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    @EntityGraph(attributePaths = {"resume", "job", "matchedRequirements",
            "missingRequirements", "recommendations"})
    Optional<Analysis> findWithDetailsById(UUID id);

    /**
     * Historico com filtros opcionais.
     *
     * `:id IS NULL OR campo = :id` e o truque para filtro opcional em JPQL:
     * quando o parametro nao vem, a condicao vira sempre verdadeira e o
     * filtro simplesmente nao se aplica. Evita escrever quatro metodos
     * diferentes para as quatro combinacoes de filtro.
     *
     * `join fetch` traz curriculo e vaga na mesma consulta — sem isso, uma
     * pagina de 20 analises dispararia 40 consultas extras (problema N+1).
     */
    @Query("""
            SELECT a FROM Analysis a
            JOIN FETCH a.resume r
            JOIN FETCH a.job j
            WHERE (:resumeId IS NULL OR r.id = :resumeId)
              AND (:jobId    IS NULL OR j.id = :jobId)
            ORDER BY a.createdAt DESC
            """)
    Page<Analysis> findHistory(@Param("resumeId") UUID resumeId,
                              @Param("jobId") UUID jobId,
                              Pageable pageable);
}
