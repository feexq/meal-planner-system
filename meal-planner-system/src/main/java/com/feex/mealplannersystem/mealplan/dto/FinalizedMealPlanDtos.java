package com.feex.mealplannersystem.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

/**
 * DTOs for the finalized meal plan returned by the Python /finalize endpoint.
 */
public class FinalizedMealPlanDtos {

    @Getter @AllArgsConstructor
    public static class FinalizedMealPlanDto {
        private String userId;
        private List<FinalizedDayDto> days;
    }

    @Getter @AllArgsConstructor
    public static class FinalizedDayDto {
        private int day;
        private FinalizedMealDto breakfast;
        private FinalizedMealDto lunch;
        private FinalizedMealDto dinner;
        private FinalizedMealDto snack;
        private FinalizedMealDto snack_2;
        /** 3-4 sentence explanation from Gemini why this day fits the user */
        private String notes;
    }

    @Getter @AllArgsConstructor
    public static class FinalizedMealDto {
        private long recipeId;
        private String name;
    }
}
