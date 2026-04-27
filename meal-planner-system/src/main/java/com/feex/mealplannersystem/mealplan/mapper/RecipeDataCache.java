package com.feex.mealplannersystem.mealplan.mapper;

import com.feex.mealplannersystem.mealplan.mapper.context.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class RecipeDataCache {

    private final RecipeRepository recipeRepository;
    private final TransactionTemplate transactionTemplate;
    private final RecipeDataAdapter  adapter;

    private RecipeDataContext cachedContext;

    @PostConstruct
    public void init() {
        transactionTemplate.executeWithoutResult(s -> refreshCache());
    }

    public RecipeDataContext buildContext() {
        if (cachedContext == null) {
            transactionTemplate.executeWithoutResult(s -> refreshCache());
        }
        return cachedContext;
    }

    public void refreshCache() {
        List<RecipeEntity> entities = recipeRepository.findAllForGenerator();
        List<RecipeModel> recipes = new ArrayList<>();
        Map<Long, NutritionModel> nutritionMap = new HashMap<>();

        for (RecipeEntity entity : entities) {
            recipes.add(adapter.toRecipeModel(entity));
            if (entity.getNutrition() != null) {
                nutritionMap.put(entity.getId(),
                        adapter.toNutritionModel(entity.getNutrition()));
            }
        }
        cachedContext = new RecipeDataContext(recipes, nutritionMap);
    }
}
