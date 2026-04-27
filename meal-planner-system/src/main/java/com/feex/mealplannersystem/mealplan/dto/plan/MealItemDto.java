package com.feex.mealplannersystem.mealplan.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MealItemDto {
    private long recipeId;
    private String name;
    private double servings;
    private double estimatedCalories;
    private String portionNote;
    private double proteinG;
    private double carbsG;
    private double fatG;
}
