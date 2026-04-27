package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.profile.achievement.AchievementResponse;
import com.feex.mealplannersystem.repository.entity.profile.AchievementEntity;
import com.feex.mealplannersystem.repository.entity.profile.UserAchievementEntity;
import org.springframework.stereotype.Component;

@Component
public class AchievementMapper {

    public AchievementResponse toDto(AchievementEntity achievement, UserAchievementEntity userAchievement) {
        boolean achieved = userAchievement != null && userAchievement.getAchievedAt() != null;
        Integer progress = userAchievement != null ? userAchievement.getProgress() : 0;

        return AchievementResponse.builder()
                .id(achievement.getId())
                .key(achievement.getKey())
                .title(achievement.getTitle())
                .description(achievement.getDescription())
                .iconUrl(achievement.getIconUrl())
                .targetValue(achievement.getTargetValue())
                .type(achievement.getType().name())
                .isAchieved(achieved)
                .currentProgress(progress)
                .achievedAt(userAchievement != null ? userAchievement.getAchievedAt() : null)
                .build();
    }
}
