package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.profile.UserAchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, Long> {

    List<UserAchievementEntity> findAllByUserId(Long userId);

    @Query("SELECT ua FROM UserAchievementEntity ua WHERE ua.user.id = :userId AND ua.achievement.id = :achievementId")
    Optional<UserAchievementEntity> findByUserIdAndAchievementId(@Param("userId") Long userId, @Param("achievementId") Long achievementId);
}
