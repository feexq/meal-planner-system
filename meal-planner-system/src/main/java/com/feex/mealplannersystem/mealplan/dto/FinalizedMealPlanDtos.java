package com.feex.mealplannersystem.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

public class FinalizedMealPlanDtos{

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class FinalizedMealPlanDto {
        private String userId;
        private List<FinalizedDayDto> days;
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class FinalizedDayDto {
        private int day;
        private int dailyCalorieTarget;
        private List<FinalizedSlotDto> slots;
        private double dayTotalCalories;
        private double dailyProteinG;
        private double dailyCarbsG;
        private double dailyFatG;
        private String notes;
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class FinalizedSlotDto {
        private String mealType;
        private int calorieBudget;
        private MealItemDto main;
        private MealItemDto side;
        private double slotTotalCalories;
        private double slotProteinG;
        private double slotCarbsG;
        private double slotFatG;
    }

    @Getter @AllArgsConstructor @NoArgsConstructor
    public static class MealItemDto {
        private long recipeId;
        private String name;
        private double servings;
        private double estimatedCalories;
        private String portionNote;
        private double proteinG;
        private double carbsG;
        private double fatG;
    }
}