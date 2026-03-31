package com.feex.mealplannersystem.mealplan.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

/**
 * Payload sent from Spring to Python /finalize.
 * Mirrors FinalizeRequest in main.py (Pydantic model).
 */
@Getter
@Builder
public class FinalizeRequestDto {

    private String userId;
    private int dailyCalorieTarget;
    private int weeklyCalorieTarget;
    private List<MealPlanDtos.DayPlanDto> days;   // reuse existing DayPlanDto
    private UserProfilePayload userProfile;

    @Getter
    @Builder
    public static class UserProfilePayload {
        private String gender;
        private int age;
        private double weightKg;
        private int heightCm;
        private String activityLevel;
        private String goal;
        private String dietType;
        private List<String> healthConditions;
        private List<String> allergies;
        private List<String> dislikedIngredients;
        private int mealsPerDay;
    }
}
