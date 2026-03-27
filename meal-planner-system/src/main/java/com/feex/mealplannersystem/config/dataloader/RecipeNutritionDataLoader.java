package com.feex.mealplannersystem.config.dataloader;

import com.feex.mealplannersystem.repository.RecipeNutritionRepository;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(3)
public class RecipeNutritionDataLoader implements ApplicationRunner {

    private final RecipeNutritionRepository nutritionRepository;
    private final ObjectMapper objectMapper;
    private final RecipeRepository recipeRepository;
    private final RecipeNutritionBatchService batchService;

    @Value("classpath:data/nutrition.json")
    private Resource nutritionFile;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (nutritionRepository.count() > 0) {
            log.info("Nutrition data already loaded, skipping...");
            return;
        }

        log.info("Loading recipe nutrition data...");

        Set<Long> existingRecipeIds = recipeRepository.findAll()
                .stream()
                .map(RecipeEntity::getId)
                .collect(Collectors.toSet());

        Map<String, Map<String, Object>> rawData = objectMapper.readValue(
                nutritionFile.getInputStream(),
                new TypeReference<>() {}
        );

        List<RecipeNutritionEntity> batch = new ArrayList<>();
        int total = 0;
        int skipped = 0;

        for (Map.Entry<String, Map<String, Object>> entry : rawData.entrySet()) {
            try {
                Long recipeId = Long.parseLong(entry.getKey());

                if (!existingRecipeIds.contains(recipeId)) {
                    skipped++;
                    continue;
                }

                Map<String, Object> nutrition = (Map<String, Object>) entry.getValue().get("nutrition");
                if (nutrition == null) continue;

                batch.add(RecipeNutritionEntity.builder()
                        .recipeId(recipeId)
                        .recipe(recipeRepository.getReferenceById(recipeId))
                        .servingSize(getString(nutrition, "serving_size"))
                        .servingsPerRecipe(getInt(nutrition, "servings_per_recipe"))
                        .calories(getDecimal(nutrition, "calories"))
                        .caloriesFromFat(getDecimal(nutrition, "calories_from_fat"))
                        .totalFatG(getDecimal(nutrition, "total_fat_g"))
                        .saturatedFatG(getDecimal(nutrition, "saturated_fat_g"))
                        .cholesterolMg(getDecimal(nutrition, "cholesterol_mg"))
                        .sodiumMg(getDecimal(nutrition, "sodium_mg"))
                        .totalCarbsG(getDecimal(nutrition, "total_carbs_g"))
                        .dietaryFiberG(getDecimal(nutrition, "dietary_fiber_g"))
                        .sugarsG(getDecimal(nutrition, "sugars_g"))
                        .proteinG(getDecimal(nutrition, "protein_g"))
                        .build());

                if (batch.size() >= 500) {
                    batchService.saveBatch(batch);
                    total += batch.size();
                    batch.clear();
                    log.info("Saved {} nutrition records...", total);
                }
            } catch (Exception e) {
                log.warn("Failed to parse nutrition for recipe {}: {}", entry.getKey(), e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            batchService.saveBatch(batch);
            total += batch.size();
        }

        log.info("Done! Loaded {} nutrition records, skipped {} (recipe not found)", total, skipped);
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? Integer.valueOf(val.toString().split("\\.")[0]) : null;
    }

    private BigDecimal getDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? new BigDecimal(val.toString()) : null;
    }
}
