package com.feex.mealplannersystem.mealplan.mapper;

import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Replaces prototype ClassifiedIngredientsService + IngredientLookupService.
 *
 * Uses IngredientRepository.findAllWithDetails() — already defined in your repo:
 *   SELECT DISTINCT i FROM IngredientEntity i
 *   LEFT JOIN FETCH i.dietaryTags dt
 *   LEFT JOIN FETCH dt.condition
 *   LEFT JOIN FETCH i.aliases
 *
 * Data model:
 *   IngredientEntity.normalizedName        → canonical ingredient name
 *   IngredientAliasEntity.rawName          → variant/alias → maps to normalizedName
 *   IngredientDietaryTagEntity.condition.id → condition key (e.g. "diabetes", "vegan")
 *   IngredientDietaryTagEntity.status      → DietaryTagStatus enum (HARD_FORBIDDEN / SOFT_FORBIDDEN)
 */
@Component
@RequiredArgsConstructor
public class IngredientClassificationAdapter {

    private final IngredientRepository ingredientRepository;

    public ClassificationContext buildContext() {
        List<IngredientEntity> all = ingredientRepository.findAllWithDetails();

        // alias/rawName → normalizedName
        Map<String, String> normMap = new HashMap<>();
        // normalizedName → { conditionId → status string }
        Map<String, Map<String, String>> tagsMap = new HashMap<>();

        for (IngredientEntity ing : all) {
            String canonical = ing.getNormalizedName().toLowerCase().trim();
            normMap.put(canonical, canonical);

            // aliases: rawName → canonical
            if (ing.getAliases() != null) {
                for (IngredientAliasEntity alias : ing.getAliases()) {
                    if (alias.getRawName() != null)
                        normMap.put(alias.getRawName().toLowerCase().trim(), canonical);
                }
            }

            // dietary tags: conditionId → status
            if (ing.getDietaryTags() != null && !ing.getDietaryTags().isEmpty()) {
                Map<String, String> condMap = new HashMap<>();
                for (IngredientDietaryTagEntity tag : ing.getDietaryTags()) {
                    if (tag.getCondition() == null || tag.getStatus() == null) continue;
                    // DietaryConditionEntity.id is a String (e.g. "diabetes", "vegan")
                    String conditionKey = tag.getCondition().getId().toLowerCase();
                    // DietaryTagStatus enum → lowercase string for compatibility with filter/scorer
                    String statusStr = tag.getStatus().name().toLowerCase(); // HARD_FORBIDDEN → "hard_forbidden"
                    condMap.put(conditionKey, statusStr);
                }
                if (!condMap.isEmpty()) tagsMap.put(canonical, condMap);
            }
        }

        return new ClassificationContext(normMap, tagsMap);
    }

    // -----------------------------------------------------------------------

    /**
     * Immutable snapshot used during one plan-generation request.
     * Exposes the same API as the prototype ClassifiedIngredientsService.
     */
    public static class ClassificationContext {

        private final Map<String, String>              normMap;
        private final Map<String, Map<String, String>> tagsMap;

        ClassificationContext(Map<String, String> normMap, Map<String, Map<String, String>> tagsMap) {
            this.normMap = normMap;
            this.tagsMap = tagsMap;
        }

        public String normalize(String raw) {
            String lower = raw.toLowerCase().trim();
            return normMap.getOrDefault(lower, lower);
        }

        private String getStatus(String rawIngredient, String condition) {
            Map<String, String> condTags = tagsMap.get(normalize(rawIngredient));
            if (condTags == null) return null;
            return condTags.get(condition.toLowerCase());
        }

        /** True if any ingredient is hard_forbidden for this condition/diet. */
        public boolean hasForbiddenIngredient(List<String> ingredients, String condition) {
            if (ingredients == null || condition == null) return false;
            for (String ing : ingredients)
                if ("hard_forbidden".equals(getStatus(ing, condition))) return true;
            return false;
        }

        /** True if any ingredient is not suitable for this diet (stored as hard_forbidden). */
        public boolean isNotSuitableFor(List<String> ingredients, String diet) {
            return hasForbiddenIngredient(ingredients, diet);
        }

        /** Returns all ingredients that are soft_forbidden for a condition. */
        public List<String> getSoftForbiddenMatches(List<String> ingredients, String condition) {
            if (ingredients == null || condition == null) return Collections.emptyList();
            Set<String> seen = new LinkedHashSet<>();
            List<String> result = new ArrayList<>();
            for (String ing : ingredients) {
                if ("soft_forbidden".equals(getStatus(ing, condition))) {
                    String norm = normalize(ing);
                    if (seen.add(norm)) result.add(ing);
                }
            }
            return result;
        }
    }
}
