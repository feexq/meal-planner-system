package com.feex.mealplannersystem.dto.mealplan;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfilePayload {
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
