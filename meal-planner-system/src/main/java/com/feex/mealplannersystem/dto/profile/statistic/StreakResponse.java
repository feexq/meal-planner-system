package com.feex.mealplannersystem.dto.profile.statistic;

import com.feex.mealplannersystem.common.user.StreakType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class StreakResponse {
    int currentStreak;
    int longestStreak;
    int totalActiveDays;
    int freezesAvailable;
    int freezesUsedThisMonth;
    StreakType streakType;
    LocalDate lastActiveDate;
}
