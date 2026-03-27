package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.repository.entity.auth.RefreshTokenEntity;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshTokenEntity createRefreshToken(UserEntity user);
    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByUser(UserEntity user);
    RefreshTokenEntity verifyExpiration(RefreshTokenEntity token);
}
