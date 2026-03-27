package com.feex.mealplannersystem.config.dataloader;

import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientTagsBatchService {

    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional
    public void saveBatch(List<IngredientDietaryTagEntity> batch) {
        for (IngredientDietaryTagEntity entity : batch) {
            entityManager.persist(entity); // Примусовий INSERT без попереднього SELECT
        }

        entityManager.flush();
        entityManager.clear();
    }
}