package com.portfolio.cvanalyzer.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    @EntityGraph(attributePaths = "requirements")
    Optional<Job> findWithRequirementsById(UUID id);

    @EntityGraph(attributePaths = "requirements")
    Page<Job> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
