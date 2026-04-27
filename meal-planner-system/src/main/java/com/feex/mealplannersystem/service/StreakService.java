package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.common.user.StreakType;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserStreakMetaEntity;

import java.time.LocalDate;

public interface StreakService {
    UserStreakMetaEntity evaluate(UserEntity user, DailyNutritionSummaryEntity summary, LocalDate today);
    UserStreakMetaEntity getStreak(Long userId);
    void changeStreakType(UserEntity user, StreakType newType);
    void resetExpiredStreaks();
    void refillMonthlyFreezes();
}
