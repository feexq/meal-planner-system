package com.feex.mealplannersystem.service.mapper;

import com.feex.mealplannersystem.dto.mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.dto.mealplan.score.ScoredAdditional;
import org.springframework.stereotype.Component;

@Component
public class AdditionalRecipeMapper {

    public AdditionalRecipeDto toDto(ScoredAdditional s) {
        return new AdditionalRecipeDto(
                s.recipe().getId(),
                s.recipe().getName(),
                s.recipe().getMealType(),
                s.nutrition().getCalories(),
                s.nutrition().getProteinG(),
                s.nutrition().getTotalCarbsG(),
                s.nutrition().getTotalFatG(),
                s.recipe().getParsedIngredients(),
                s.recipe().getTags()
        );
    }
}
