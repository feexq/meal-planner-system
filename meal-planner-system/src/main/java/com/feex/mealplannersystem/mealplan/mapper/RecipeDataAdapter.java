package com.feex.mealplannersystem.mealplan.mapper;

import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecipeDataAdapter {

    public RecipeModel toRecipeModel(RecipeEntity e) {
        List<String> ingredients = e.getIngredients().stream()
                .map(i -> {
                    if (i.getIngredient() != null && i.getIngredient().getNormalizedName() != null)
                        return i.getIngredient().getNormalizedName().toLowerCase().trim();
                    return i.getRawName() != null ? i.getRawName().toLowerCase().trim() : "";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> tags = e.getTags().stream()
                .map(t -> t.getName().toLowerCase().trim())
                .collect(Collectors.toList());

        return RecipeModel.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .mealType(e.getMealType() != null ? e.getMealType().name().toLowerCase() : null)
                .cookTime(e.getCookTime() != null ? e.getCookTime().name() : null)
                .cookComplexity(e.getCookComplexity() != null ? e.getCookComplexity().name() : null)
                .cookBudget(e.getCookBudget() != null ? e.getCookBudget().name() : null)
                .servings(e.getServings() != null ? e.getServings() : 1)
                .servingSize(e.getServingSize())
                .parsedIngredients(ingredients)
                .tags(tags)
                .build();
    }

    public NutritionModel toNutritionModel(RecipeNutritionEntity n) {
        return NutritionModel.builder()
                .recipeId(n.getRecipe().getId())
                .calories(bd(n.getCalories()))
                .proteinG(bd(n.getProteinG()))
                .totalFatG(bd(n.getTotalFatG()))
                .saturatedFatG(bd(n.getSaturatedFatG()))
                .totalCarbsG(bd(n.getTotalCarbsG()))
                .dietaryFiberG(bd(n.getDietaryFiberG()))
                .sugarsG(bd(n.getSugarsG()))
                .sodiumMg(bd(n.getSodiumMg()))
                .cholesterolMg(bd(n.getCholesterolMg()))
                .servingsPerRecipe(n.getServingsPerRecipe() != null ? n.getServingsPerRecipe() : 1)
                .build();
    }

    private static double bd(BigDecimal v) {
        return v != null ? v.doubleValue() : 0.0;
    }
}
