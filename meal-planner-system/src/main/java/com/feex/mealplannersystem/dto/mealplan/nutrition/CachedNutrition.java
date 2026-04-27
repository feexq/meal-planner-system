package com.feex.mealplannersystem.dto.mealplan.nutrition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CachedNutrition {
    private double calories;
    private double proteinG;
    private double carbsG;
    private double fatG;
    private String quantityDescription;
}
