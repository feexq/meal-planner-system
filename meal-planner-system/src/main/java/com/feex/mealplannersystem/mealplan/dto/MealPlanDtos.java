package com.feex.mealplannersystem.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

public class MealPlanDtos {

    @Getter @AllArgsConstructor
    public static class WeeklyMealPlanDto {
        private String userId;
        private int dailyCalorieTarget;
        private int weeklyCalorieTarget;
        private double relaxationRate;
        private boolean lowCoverageWarning;
        private List<DayPlanDto> days;
    }

    @Getter @AllArgsConstructor
    public static class DayPlanDto {
        private int day;
        private int dailyCalorieTarget;
        private EstimatedDailyMacrosDto estimatedDailyMacros;
        private List<MealSlotDto> slots;
        private double relaxationRate;
    }

    @Getter @AllArgsConstructor
    public static class MealSlotDto {
        private String mealType;
        private int slotCalorieBudget;
        private List<RecipeCandidateDto> candidates;
        private boolean lowCoverage;
        private String filteringNote;
    }

    @Getter @AllArgsConstructor
    public static class EstimatedDailyMacrosDto {
        private int proteinG;
        private int carbsG;
        private int fatG;
        private int proteinTargetG;
        private int proteinMinG;
        private int proteinMaxG;
        private int fatTargetG;
        private int fatMinG;
        private int fatMaxG;
        private int carbsTargetG;
        private int carbsAbsoluteMinG;
        private boolean lowCarbWarning;
        private int proteinCoveragePercent;
        private String proteinStatus;
        private boolean criticalProteinWarning;
        private int carbsCoveragePercent;
        private String carbsStatus;
        private int fatCoveragePercent;
        private String fatStatus;
    }
}
