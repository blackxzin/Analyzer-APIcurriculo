package com.portfolio.cvanalyzer.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Existe pelo menos um administrador cadastrado? Usado no primeiro cadastro. */
    boolean existsByRole(Role role);
}
