package com.feex.mealplannersystem.dto.recipe;

import com.feex.mealplannersystem.common.mealplan.MealType;
import com.feex.mealplannersystem.common.survey.CookTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSummaryResponse {
    Long id;
    String name;
    String slug;
    String imageUrl;
    MealType mealType;
    CookTime cookTime;
    Set<String> tags;
    Double calories;
    Double proteinG;
    Double totalFatG;
    Double totalCarbsG;
}