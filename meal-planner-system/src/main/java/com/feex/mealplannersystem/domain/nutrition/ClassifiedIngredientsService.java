package com.feex.mealplannersystem.domain.nutrition; // Замініть на ваш пакет

import com.feex.mealplannersystem.common.DietaryTagStatus;
import com.feex.mealplannersystem.repository.IngredientAliasRepository;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClassifiedIngredientsService {

    private final IngredientRepository ingredientRepository;

    // ГОЛОВНА МАПА: Пошук по ID нормалізованого інгредієнта
    private Map<Long, IngredientEntity> ingredientByIdCache;

    // ЗАПАСНА МАПА: Пошук по сирому тексту (якщо ingredient_id раптом NULL)
    private Map<String, IngredientEntity> ingredientByAliasCache;

    public ClassifiedIngredientsService(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @PostConstruct
    public void initCache() {
        List<IngredientEntity> ingredients = ingredientRepository.findAllWithDetails();

        ingredientByIdCache = new HashMap<>();
        ingredientByAliasCache = new HashMap<>();

        int totalTagsLoaded = 0;

        for (IngredientEntity ing : ingredients) {
            // 1. Кешуємо по ID (Найнадійніший спосіб)
            if (ing.getId() != null) {
                ingredientByIdCache.put(ing.getId(), ing);
            }

            // 2. Кешуємо по синонімах для підстраховки
            if (ing.getAliases() != null) {
                for (IngredientAliasEntity alias : ing.getAliases()) {
                    if (alias.getRawName() != null) {
                        ingredientByAliasCache.put(alias.getRawName().toLowerCase().trim(), ing);
                    }
                }
            }

            if (ing.getDietaryTags() != null) {
                totalTagsLoaded += ing.getDietaryTags().size();
            }
        }

        System.out.println("✅ ClassifiedIngredientsService: Кеш ідеально завантажено!");
        System.out.println("   -> Нормалізованих інгредієнтів по ID: " + ingredientByIdCache.size());
        System.out.println("   -> Синонімів для текстового пошуку: " + ingredientByAliasCache.size());
        System.out.println("   -> Загалом тегів здоров'я: " + totalTagsLoaded);
    }

    public boolean hasForbiddenIngredient(Set<RecipeIngredientEntity> ingredients, String conditionName) {
        if (ingredients == null || conditionName == null) return false;
        return ingredients.stream()
                .anyMatch(ing -> hasStatus(ing, conditionName, DietaryTagStatus.HARD_FORBIDDEN));
    }

    public boolean hasSoftForbiddenIngredient(Set<RecipeIngredientEntity> ingredients, String conditionName) {
        if (ingredients == null || conditionName == null) return false;
        return ingredients.stream()
                .anyMatch(ing -> hasStatus(ing, conditionName, DietaryTagStatus.SOFT_FORBIDDEN));
    }

    public List<String> getHardForbiddenMatches(Set<RecipeIngredientEntity> ingredients, String conditionName) {
        return getMatchesByStatus(ingredients, conditionName, DietaryTagStatus.HARD_FORBIDDEN);
    }

    public List<String> getSoftForbiddenMatches(Set<RecipeIngredientEntity> ingredients, String conditionName) {
        return getMatchesByStatus(ingredients, conditionName, DietaryTagStatus.SOFT_FORBIDDEN);
    }

    private boolean hasStatus(RecipeIngredientEntity recipeIngredient, String conditionName, DietaryTagStatus targetStatus) {
        IngredientEntity normalFormIngredient = null;

        if (recipeIngredient.getIngredient() != null && recipeIngredient.getIngredient().getId() != null) {
            normalFormIngredient = ingredientByIdCache.get(recipeIngredient.getIngredient().getId());
        }

        if (normalFormIngredient == null && recipeIngredient.getRawName() != null) {
            String aliasName = recipeIngredient.getRawName().toLowerCase().trim();
            normalFormIngredient = ingredientByAliasCache.get(aliasName);
        }

        if (normalFormIngredient == null || normalFormIngredient.getDietaryTags() == null) {
            return false;
        }

        return normalFormIngredient.getDietaryTags().stream()
                .anyMatch(tag -> {
                    if (tag.getCondition() == null || tag.getStatus() != targetStatus) return false;

                    String dbName = tag.getCondition().getId() != null ? tag.getCondition().getId().toLowerCase() : "";

                    return dbName.equals(conditionName);
                });
    }

    private List<String> getMatchesByStatus(Set<RecipeIngredientEntity> ingredients, String conditionName, DietaryTagStatus targetStatus) {
        if (ingredients == null || conditionName == null) return List.of();

        return ingredients.stream()
                .filter(ing -> hasStatus(ing, conditionName, targetStatus))
                .map(RecipeIngredientEntity::getRawName)
                .distinct()
                .collect(Collectors.toList());
    }

}