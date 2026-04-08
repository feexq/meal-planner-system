package com.feex.mealplannersystem.dto.profile;

import com.feex.mealplannersystem.common.StreakType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileSummaryDto {
    String userId;
    String firstName;
    String lastName;
    String email;
    String avatarUrl;
    String timezone;
    String bio;
    Integer age;

    Double currentWeightKg;
    Double targetWeightKg;
    Double weightProgressPercent;

    Integer currentStreak;
    Integer longestStreak;
    Integer totalActiveDays;
    Integer freezesAvailable;
    StreakType streakType;

    WeeklyAveragesDto weeklyAverages;
}
