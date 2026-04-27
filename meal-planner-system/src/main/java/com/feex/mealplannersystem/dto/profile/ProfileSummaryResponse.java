package com.feex.mealplannersystem.dto.profile;

import com.feex.mealplannersystem.common.user.StreakType;
import com.feex.mealplannersystem.dto.profile.statistic.WeeklyAveragesResponse;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileSummaryResponse {
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
    WeeklyAveragesResponse weeklyAverages;
}
