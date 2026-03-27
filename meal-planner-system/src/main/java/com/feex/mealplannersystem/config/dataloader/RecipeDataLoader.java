package com.feex.mealplannersystem.config.dataloader;

import com.feex.mealplannersystem.common.survey.CookBudget;
import com.feex.mealplannersystem.common.survey.CookComplexity;
import com.feex.mealplannersystem.common.survey.CookTime;
import com.feex.mealplannersystem.common.MealType;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.IngredientAliasRepository;
import com.feex.mealplannersystem.repository.RecipeRepository;
import com.feex.mealplannersystem.repository.TagRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeStepEntity;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class RecipeDataLoader implements ApplicationRunner {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;
    private final ObjectMapper objectMapper;
    private final TagRepository tagRepository;

    @Value("classpath:data/recipes.json")
    private Resource recipesFile;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (recipeRepository.count() > 0) {
            log.info("Recipes already loaded, skipping...");
            return;
        }

        log.info("Loading recipes from JSON...");

        Map<String, Long> ingredientMap = ingredientRepository.findAll()
                .stream()
                .filter(i -> i.getNormalizedName() != null)
                .collect(Collectors.toMap(
                        i -> cleanSearchKey(i.getNormalizedName()),
                        IngredientEntity::getId,
                        (a, b) -> a
                ));

        log.info("Loaded {} ingredients into lookup map", ingredientMap.size());

        Map<String, Long> aliasMap = ingredientAliasRepository.findAll()
                .stream()
                .filter(a -> a.getRawName() != null)
                .collect(Collectors.toMap(
                        alias -> cleanSearchKey(alias.getRawName()),
                        alias -> alias.getIngredient().getId(),
                        (a, b) -> a
                ));

        log.info("Loaded {} ingredient aliases into lookup map", aliasMap.size());

        List<Map<String, Object>> rawRecipes = objectMapper.readValue(
                recipesFile.getInputStream(),
                new TypeReference<>() {}
        );

        int batchSize = 100;
        List<RecipeEntity> batch = new ArrayList<>();
        int total = 0;

        for (Map<String, Object> raw : rawRecipes) {
            try {
                RecipeEntity recipe = mapToEntity(raw, ingredientMap, aliasMap);
                batch.add(recipe);

                if (batch.size() >= batchSize) {
                    recipeRepository.saveAll(batch);
                    total += batch.size();
                    batch.clear();
                    log.info("Saved {} recipes...", total);
                }
            } catch (Exception e) {
                log.warn("Failed to parse recipe {}: {}", raw.get("id"), e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            recipeRepository.saveAll(batch);
            total += batch.size();
        }

        log.info("Done! Loaded {} recipes total", total);
    }

    private RecipeEntity mapToEntity(Map<String, Object> raw, Map<String, Long> ingredientMap, Map<String, Long> aliasMap) {
        Long id = Long.valueOf(raw.get("id").toString());
        String name = (String) raw.get("name");
        String slug = toSlug(name) + "-" + id;

        RecipeEntity recipe = RecipeEntity.builder()
                .id(id)
                .name(name)
                .description((String) raw.get("description"))
                .slug(slug)
                .servingSize((String) raw.get("serving_size"))
                .servings(raw.get("servings") != null ? Integer.valueOf(raw.get("servings").toString()) : null)
                .mealType(parseMealType((String) raw.get("meal_type")))
                .mealTypeDetailed((String) raw.get("meal_type_detailed"))
                .cookTime(CookTime.fromString((String) raw.get("cook_time")))
                .cookComplexity(CookComplexity.valueOf(raw.get("cook_complexity").toString().toUpperCase()))
                .cookBudget(CookBudget.valueOf(raw.get("cook_budget").toString().toUpperCase()))
                .build();

        String stepsStr = (String) raw.get("steps");
        List<String> steps = parseStringList(stepsStr);
        for (int i = 0; i < steps.size(); i++) {
            recipe.getSteps().add(RecipeStepEntity.builder()
                    .recipe(recipe)
                    .stepNumber(i + 1)
                    .description(steps.get(i))
                    .build());
        }

        String ingredientsStr = (String) raw.get("ingredients");
        List<String> normalizedIngredients = parseStringList(ingredientsStr);

        for (String rawName : normalizedIngredients) {
            String searchKey = cleanSearchKey(rawName);

            Long ingredientId = ingredientMap.get(searchKey);

            if (ingredientId == null) {
                ingredientId = aliasMap.get(searchKey);

                if (ingredientId == null) {
                    log.warn("❌ Не знайдено ID для: [{}] (Оригінал: [{}])", searchKey, rawName);
                }
            }

            IngredientEntity ingredientRef = ingredientId != null
                    ? IngredientEntity.builder().id(ingredientId).build()
                    : null;

            recipe.getIngredients().add(RecipeIngredientEntity.builder()
                    .recipe(recipe)
                    .ingredient(ingredientRef)
                    .rawName(rawName)
                    .build());
        }

        Map<String, TagEntity> tagMap = tagRepository.findAll()
                .stream()
                .collect(Collectors.toMap(TagEntity::getName, t -> t));

        List<Object> tagsList = (List<Object>) raw.get("tags");
        if (tagsList != null) {
            tagsList.forEach(t -> {
                TagEntity tag = tagMap.get(t.toString());
                if (tag != null) {
                    recipe.getTags().add(tag);
                }
            });
        }

        return recipe;
    }

    private String cleanSearchKey(String input) {
        if (input == null || input.isBlank()) return "";
        return input.toLowerCase()
                .replace("%25", "%")
                .replace("%26", "&")
                .replaceAll("[\"']", "")
                .trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            try {
                String cleaned = raw.trim();
                cleaned = cleaned.substring(1, cleaned.length() - 1);
                return Arrays.stream(cleaned.split(","))
                        .map(s -> s.trim().replaceAll("^'|'$", ""))
                        .filter(s -> !s.isBlank())
                        .toList();
            } catch (Exception ex) {
                log.warn("Failed to parse list: {}", raw);
                return new ArrayList<>();
            }
        }
    }

    private MealType parseMealType(String value) {
        if (value == null) return MealType.UNCLASSIFIED;
        return switch (value) {
            case "breakfast"        -> MealType.BREAKFAST;
            case "lunch"            -> MealType.LUNCH;
            case "dinner"           -> MealType.DINNER;
            case "dessert"          -> MealType.DESSERT;
            case "drink"            -> MealType.DRINK;
            case "snack"            -> MealType.SNACK;
            case "sauce_or_condiment" -> MealType.SAUCE_OR_CONDIMENT;
            default                 -> MealType.UNCLASSIFIED;
        };
    }

    private String toSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}