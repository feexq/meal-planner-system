package com.feex.mealplannersystem.config.dataloader;

import com.feex.mealplannersystem.repository.RecipeNutritionRepository;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeNutritionBatchService {

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional
    public void saveBatch(List<RecipeNutritionEntity> batch) {
        for (int i = 0; i < batch.size(); i++) {
            entityManager.persist(batch.get(i));
        }
        entityManager.flush();
        entityManager.clear();
    }
}
