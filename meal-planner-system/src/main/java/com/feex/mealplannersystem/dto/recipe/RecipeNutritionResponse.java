package com.feex.mealplannersystem.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeNutritionResponse {
    private String servingSize;
    private Integer servingsPerRecipe;
    private BigDecimal calories;
    private BigDecimal caloriesFromFat;
    private BigDecimal totalFatG;
    private BigDecimal saturatedFatG;
    private BigDecimal cholesterolMg;
    private BigDecimal sodiumMg;
    private BigDecimal totalCarbsG;
    private BigDecimal dietaryFiberG;
    private BigDecimal sugarsG;
    private BigDecimal proteinG;
}
