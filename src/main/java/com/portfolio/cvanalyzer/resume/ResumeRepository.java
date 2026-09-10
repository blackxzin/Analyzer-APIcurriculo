package com.portfolio.cvanalyzer.resume;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Acesso a dados de curriculo.
 *
 * Nao escrevemos implementacao: o Spring Data gera a classe em tempo de
 * execucao a partir da assinatura dos metodos. Menos codigo, menos bug.
 *
 * @EntityGraph resolve o problema N+1: sem ele, buscar 1 curriculo e depois
 * ler suas tecnologias e secoes dispararia consultas extras. Com ele, tudo
 * vem numa consulta so.
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    @EntityGraph(attributePaths = {"technologies", "sectionItems"})
    Optional<Resume> findWithDetailsById(UUID id);

    Page<Resume> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByEmail(String email);
}
