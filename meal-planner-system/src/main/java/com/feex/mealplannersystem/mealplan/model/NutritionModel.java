package com.feex.mealplannersystem.mealplan.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Flat nutrition data used by the scoring/filtering logic.
 * Replaces prototype NutritionEntry + NutritionInfo.
 */
@Getter
@Builder
public class NutritionModel {

    private long recipeId;
    private double calories;
    private double proteinG;
    private double totalFatG;
    private double saturatedFatG;
    private double totalCarbsG;
    private double dietaryFiberG;
    private double sugarsG;
    private double sodiumMg;
    private double cholesterolMg;
    private int servingsPerRecipe;
}
