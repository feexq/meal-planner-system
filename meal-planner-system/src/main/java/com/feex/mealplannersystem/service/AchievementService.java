package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.profile.achievement.AchievementResponse;
import com.feex.mealplannersystem.dto.profile.achievement.CreateAchievementRequest;
import com.feex.mealplannersystem.dto.profile.achievement.UpdateAchievementRequest;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.AchievementEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserStreakMetaEntity;

import java.util.List;

public interface AchievementService {
    List<AchievementResponse> getUserAchievements(UserEntity user);
    List<AchievementEntity> getAllAchievements();
    AchievementEntity createAchievement(CreateAchievementRequest request);
    AchievementEntity updateAchievement(Long id, UpdateAchievementRequest request);
    void deleteAchievement(Long id);
}
