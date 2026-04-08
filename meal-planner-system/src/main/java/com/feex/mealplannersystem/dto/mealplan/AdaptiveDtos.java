package com.feex.mealplannersystem.dto.mealplan;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

public class AdaptiveDtos {

    @Getter @Builder @AllArgsConstructor
    public static class LogFoodRequest {
        private String foodText;
        private int dayNumber;
    }

    @Getter @Builder @AllArgsConstructor
    public static class MarkSlotEatenRequest {
        private Long slotId;
        private Double actualCalories;
    }

    @Getter @AllArgsConstructor
    public static class LogFoodResponse {
        private List<ParsedFoodItem> parsedItems;
        private double totalCalories;
        private double totalProteinG;
        private double totalCarbsG;
        private double totalFatG;
        private String parseNote;
        private WeeklyBalanceDto weeklyBalance;
    }

    @Getter @AllArgsConstructor
    public static class ParsedFoodItem {
        private String name;
        private String original;
        private String quantityDescription;
        private double calories;
        private double proteinG;
        private double carbsG;
        private double fatG;
        private String confidence;
        private boolean fromCache;
    }

    @Getter @Builder
    public static class WeeklyBalanceDto {
        private int weeklyCalorieTarget;
        private double totalConsumed;
        private double remainingBudget;
        private int remainingDays;
        private double adjustedDailyTarget;
        private boolean overLimitWarning;
        private String adjustmentNote;
    }

    @Getter @AllArgsConstructor
    public static class AdaptedPlanResponse {
        private Long planId;
        private WeeklyBalanceDto weeklyBalance;
        private List<DayTargetDto> updatedDayTargets;
    }

    @Getter @AllArgsConstructor
    public static class DayTargetDto {
        private int dayNumber;
        private double originalTarget;
        private double adjustedTarget;
        private double delta;
        private String suggestedAction;
    }

    @Getter @Builder
    public static class PlanStatusDto {
        private Long planId;
        private String weekStartDate;
        private WeeklyBalanceDto weeklyBalance;
        private List<DayTargetDto> updatedTargets;
        private List<DayStatusDto> days;
    }

    @Getter @Builder
    public static class DayStatusDto {
        private int dayNumber;
        private double targetCalories;
        private double plannedCalories;
        private double consumedCalories;
        private double extraCalories;
        private List<SlotStatusDto> slots;
        private List<LoggedFoodDto> extraFood;
    }

    @Getter @Builder
    public static class SlotStatusDto {
        private Long slotId;
        private String mealType;
        private Long recipeId;
        private String recipeName;
        private double targetCalories;
        private Double actualCalories;
        private String status;
        private LocalDateTime eatenAt;
    }

    @Getter @Builder
    public static class LoggedFoodDto {
        private Long logId;
        private String rawInput;
        private double totalCalories;
        private String confidence;
        private LocalDateTime loggedAt;
    }
}
