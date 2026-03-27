package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, String> {

    Optional<UserPreferenceEntity> findByUserEmail(String email);

    boolean existsByUserEmail(String email);

    void deleteByUserEmail(String email);
}