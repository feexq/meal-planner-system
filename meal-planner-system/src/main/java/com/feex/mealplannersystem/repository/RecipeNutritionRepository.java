package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeNutritionRepository extends JpaRepository<RecipeNutritionEntity, Long> {}