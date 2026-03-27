package com.feex.mealplannersystem.config.normalizer;

import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.service.DietaryTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassificationScheduler {

    private final IngredientNormalizationQueue queue;
    private final IngredientRepository ingredientRepository;
    private final DietaryTagService dietaryTagService;
    private final ClassifierClient classifierClient;

    @Value("${normalizer.batch-size:35}")
    private int batchSize;

    // Запускається кожні 10 хвилин
    @Scheduled(fixedDelay = 600_000)
    public void processBatch() {
        long queueSize = queue.size();

        if (queueSize == 0) return;

        log.info("Classification queue size: {}, processing...", queueSize);

        List<Long> ids = queue.popBatch(batchSize);
        if (ids.isEmpty()) return;

        List<IngredientEntity> ingredients = ingredientRepository.findAllById(ids);
        List<String> names = ingredients.stream()
                .map(IngredientEntity::getNormalizedName)
                .toList();

        ClassifierClient.ClassifyResponse response = classifierClient.classify(names);

        for (IngredientEntity ingredient : ingredients) {
            Map<String, String> tags = response.results().get(ingredient.getNormalizedName());
            if (tags != null) {
                dietaryTagService.saveBatchTags(ingredient.getId(), tags);
                log.info("Saved tags for: {}", ingredient.getNormalizedName());
            } else {
                log.warn("No tags returned for: {}", ingredient.getNormalizedName());
                queue.add(ingredient.getId());
            }
        }
    }
}
