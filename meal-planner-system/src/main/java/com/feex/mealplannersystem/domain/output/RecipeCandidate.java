package com.feex.mealplannersystem.domain.output;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCandidate {
    private int recipeId;
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
    private ScoreBreakdown scoreBreakdown;
    private List<String> ingredients;
}
