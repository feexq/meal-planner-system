package com.feex.mealplannersystem.dto.profile;

import com.feex.mealplannersystem.common.StreakType;
import lombok.Builder;
import lombok.Data;
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
