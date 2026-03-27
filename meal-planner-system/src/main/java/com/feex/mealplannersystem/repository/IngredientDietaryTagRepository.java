package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import com.feex.mealplannersystem.repository.other.IngredientDietaryTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientDietaryTagRepository extends JpaRepository<IngredientDietaryTagEntity, IngredientDietaryTagId> {

    @Query("SELECT t FROM IngredientDietaryTagEntity t JOIN FETCH t.condition WHERE t.id.ingredientId = :ingredientId")
    List<IngredientDietaryTagEntity> findAllByIngredientId(@Param("ingredientId") Long ingredientId);

    @Modifying
    @Query("DELETE FROM IngredientDietaryTagEntity t WHERE t.id.ingredientId = :ingredientId")
    void deleteAllByIngredientId(@Param("ingredientId") Long ingredientId);

    @Modifying
    @Query(value = """
    INSERT INTO ingredient_dietary_tags (ingredient_id, condition_id, status)
    VALUES (:ingredientId, :conditionId, :status)
    """, nativeQuery = true)
    void insertTag(
            @Param("ingredientId") Long ingredientId,
            @Param("conditionId") String conditionId,
            @Param("status") String status
    );
}
