package com.feex.mealplannersystem.config.dataloader;

import com.feex.mealplannersystem.common.mealplan.DietaryTagStatus;
import com.feex.mealplannersystem.repository.DietaryConditionRepository;
import com.feex.mealplannersystem.repository.IngredientDietaryTagRepository;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.DietaryConditionEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import com.feex.mealplannersystem.repository.other.IngredientDietaryTagId;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class IngredientTagsDataLoader implements ApplicationRunner {

    private final IngredientRepository ingredientRepository;
    private final IngredientDietaryTagRepository tagRepository;
    private final DietaryConditionRepository conditionRepository;
    private final ObjectMapper objectMapper;

    private final IngredientTagsBatchService batchService;

    @Value("classpath:data/ingredient_tags.json")
    private Resource tagsFile;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (tagRepository.count() > 100) {
            log.info("Ingredient tags already loaded, skipping...");
            return;
        }

        log.info("Loading ingredient dietary tags...");

        Map<String, Long> ingredientMap = ingredientRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        i -> i.getNormalizedName(),
                        i -> i.getId(),
                        (a, b) -> a
                ));

        Map<String, String> conditionMap = conditionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        DietaryConditionEntity::getId,
                        DietaryConditionEntity::getId
                ));

        Map<String, Map<String, String>> tagsData = objectMapper.readValue(
                tagsFile.getInputStream(),
                new TypeReference<>() {}
        );

        List<IngredientDietaryTagEntity> batch = new ArrayList<>();
        int total = 0;

        for (Map.Entry<String, Map<String, String>> entry : tagsData.entrySet()) {
            String normalizedName = entry.getKey();
            Long ingredientId = ingredientMap.get(normalizedName);

            if (ingredientId == null) continue;

            for (Map.Entry<String, String> tagEntry : entry.getValue().entrySet()) {
                String conditionId = tagEntry.getKey();
                String statusStr = tagEntry.getValue().toUpperCase();

                if (!conditionMap.containsKey(conditionId)) continue;

                DietaryTagStatus status;
                try {
                    status = DietaryTagStatus.valueOf(statusStr);
                } catch (Exception e) {
                    continue;
                }

                batch.add(IngredientDietaryTagEntity.builder()
                        .id(new IngredientDietaryTagId(ingredientId, conditionId))
                        .ingredient(ingredientRepository.getReferenceById(ingredientId))
                        .condition(conditionRepository.getReferenceById(conditionId))
                        .status(status)
                        .build());

                if (batch.size() >= 500) {
                    batchService.saveBatch(batch);
                    total += batch.size();
                    batch.clear();
                    log.info("Saved {} tags...", total);
                }
            }
        }

        if (!batch.isEmpty()) {
            batchService.saveBatch(batch);
            total += batch.size();
        }

        log.info("Done! Loaded {} dietary tags", total);
    }
}