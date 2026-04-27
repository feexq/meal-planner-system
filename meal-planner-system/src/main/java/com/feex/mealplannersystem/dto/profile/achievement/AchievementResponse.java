package com.feex.mealplannersystem.dto.profile.achievement;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AchievementResponse {
    Long id;
    String key;
    String title;
    String description;
    String iconUrl;
    Integer targetValue;
    String type;
    boolean isAchieved;
    Integer currentProgress;
    LocalDateTime achievedAt;
}
