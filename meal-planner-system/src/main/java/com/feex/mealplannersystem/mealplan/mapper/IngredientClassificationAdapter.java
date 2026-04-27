package com.feex.mealplannersystem.mealplan.mapper;

import com.feex.mealplannersystem.mealplan.mapper.context.ClassificationContext;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class IngredientClassificationAdapter {

    private final IngredientRepository ingredientRepository;

    public ClassificationContext buildContext() {
        List<IngredientEntity> all = ingredientRepository.findAllWithDetails();

        Map<String, String> normMap = new HashMap<>();
        Map<String, Map<String, String>> tagsMap = new HashMap<>();

        for (IngredientEntity ing : all) {
            String canonical = ing.getNormalizedName().toLowerCase().trim();
            normMap.put(canonical, canonical);

            if (ing.getAliases() != null) {
                for (IngredientAliasEntity alias : ing.getAliases()) {
                    if (alias.getRawName() != null)
                        normMap.put(alias.getRawName().toLowerCase().trim(), canonical);
                }
            }

            if (ing.getDietaryTags() != null && !ing.getDietaryTags().isEmpty()) {
                Map<String, String> condMap = new HashMap<>();
                for (IngredientDietaryTagEntity tag : ing.getDietaryTags()) {
                    if (tag.getCondition() == null || tag.getStatus() == null) continue;
                    String conditionKey = tag.getCondition().getId().toLowerCase();
                    String statusStr = tag.getStatus().name().toLowerCase();
                    condMap.put(conditionKey, statusStr);
                }
                if (!condMap.isEmpty()) tagsMap.put(canonical, condMap);
            }
        }

        return new ClassificationContext(normMap, tagsMap);
    }
}
