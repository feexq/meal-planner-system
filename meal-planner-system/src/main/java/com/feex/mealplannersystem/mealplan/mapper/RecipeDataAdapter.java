package com.feex.mealplannersystem.mealplan.mapper;

import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Replaces prototype DataLoader.
 *
 * Uses RecipeRepository.findAllForGenerator() — already defined in your repo:
 *   SELECT DISTINCT r FROM RecipeEntity r
 *   LEFT JOIN FETCH r.nutrition
 *   LEFT JOIN FETCH r.tags
 *   LEFT JOIN FETCH r.ingredients
 *
 * No extra queries needed — your existing query fetches everything in one go.
 * ingredients and tags are Set<> in RecipeEntity, which avoids MultipleBagFetchException.
 */
@Component
@RequiredArgsConstructor
public class RecipeDataAdapter {

    private final RecipeRepository recipeRepository;

    public RecipeDataContext buildContext() {
        List<RecipeEntity> entities = recipeRepository.findAllForGenerator();

        List<RecipeModel>         recipes      = new ArrayList<>(entities.size());
        Map<Long, NutritionModel> nutritionMap = new HashMap<>(entities.size());

        for (RecipeEntity entity : entities) {
            recipes.add(toRecipeModel(entity));
            if (entity.getNutrition() != null) {
                nutritionMap.put(entity.getId(), toNutritionModel(entity.getNutrition()));
            }
        }

        return new RecipeDataContext(recipes, nutritionMap);
    }

    // -----------------------------------------------------------------------

    private RecipeModel toRecipeModel(RecipeEntity e) {
        // ingredients: prefer resolved IngredientEntity.normalizedName, fall back to rawName
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
                // CookTime.name() returns enum constant like "THIRTY_MIN" —
                // mapped to prototype strings in RecipeScorerService.scoreCookTime()
                .cookTime(e.getCookTime() != null ? e.getCookTime().name() : null)
                .cookComplexity(e.getCookComplexity() != null ? e.getCookComplexity().name() : null)
                .cookBudget(e.getCookBudget() != null ? e.getCookBudget().name() : null)
                .servings(e.getServings() != null ? e.getServings() : 1)
                .servingSize(e.getServingSize())
                .parsedIngredients(ingredients)
                .tags(tags)
                .build();
    }

    private NutritionModel toNutritionModel(RecipeNutritionEntity n) {
        // RecipeNutritionEntity uses BigDecimal — convert to double safely
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

    // -----------------------------------------------------------------------

    public static class RecipeDataContext {
        private final List<RecipeModel>         recipes;
        private final Map<Long, NutritionModel> nutritionMap;

        public RecipeDataContext(List<RecipeModel> recipes, Map<Long, NutritionModel> nutritionMap) {
            this.recipes      = Collections.unmodifiableList(recipes);
            this.nutritionMap = Collections.unmodifiableMap(nutritionMap);
        }

        public List<RecipeModel> getRecipes()                { return recipes; }
        public NutritionModel    getNutrition(long recipeId) { return nutritionMap.get(recipeId); }
    }
}
