package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.profile.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AchievementRepository extends JpaRepository<AchievementEntity, Long> {
    Optional<AchievementEntity> findByKey(String key);
}
