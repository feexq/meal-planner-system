package com.feex.mealplannersystem.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class RecipeCandidateDto {
    private long recipeId;
    private String recipeName;
    private int score;
    private double caloriesPerServing;
    private double recommendedServings;
    private double scaledCalories;
    private double caloricDeviation;
    private List<String> matchedHealthTags;
    private List<String> appliedNutritionThresholds;
    private List<String> dietaryNotes;
    private List<String> relaxations;
    private ScoreBreakdownDto scoreBreakdown;
    private List<String> ingredients;
}
