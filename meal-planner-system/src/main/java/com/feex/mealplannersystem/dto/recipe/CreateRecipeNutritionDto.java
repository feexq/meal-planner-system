package com.feex.mealplannersystem.dto.recipe;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecipeNutritionDto {
    BigDecimal calories;
    BigDecimal proteinG;
    BigDecimal totalFatG;
    BigDecimal totalCarbsG;
}
