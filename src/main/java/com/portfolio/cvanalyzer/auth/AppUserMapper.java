package com.portfolio.cvanalyzer.auth;

import com.portfolio.cvanalyzer.auth.dto.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Traducao entidade -> DTO de saida.
 *
 * Fica numa classe propria (e nao dentro do service) para que a regra
 * "o que sai da API" tenha um unico lugar e um teste proprio.
 */
@Component
public class AppUserMapper {

    public UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
