package com.feex.mealplannersystem.mealplan.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileModel {

    private String userId;
    private String gender;
    private int age;
    private int heightCm;
    private double weightKg;
    private String activityLevel;
    private String goal;
    private String goalIntensity;
    private String dietType;
    private List<String> healthConditions;
    private List<String> allergies;
    private List<String> dislikedIngredients;
    private int mealsPerDay;
    private String cookingComplexity;
    private String budgetLevel;
    private boolean zigzag;
    private Integer maxRecipeRepeatsPerWeek;
}
