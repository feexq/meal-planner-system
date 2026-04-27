package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.projection.TopRecipeProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanSlotRepository extends JpaRepository<MealPlanSlotEntity, Long> {

    List<MealPlanSlotEntity> findByPlanIdAndDayNumber(Long planId, int dayNumber);

    @Query("""
            SELECT s FROM MealPlanSlotEntity s
            WHERE s.plan.id = :planId
            AND s.dayNumber >= :fromDay
            AND s.status = 'PLANNED'
            """)
    List<MealPlanSlotEntity> findPlannedSlotsFromDay(
            @Param("planId") Long planId,
            @Param("fromDay") int fromDay);

    List<MealPlanSlotEntity> findByPlanId(Long planId);

    @Query("""
            SELECT s.recipeId AS recipeId, s.recipeName AS recipeName, COUNT(s.id) AS count
            FROM MealPlanSlotEntity s
            JOIN s.plan p
            WHERE p.user.id = :userId AND s.status = 'EATEN' AND s.recipeId IS NOT NULL
            GROUP BY s.recipeId, s.recipeName
            ORDER BY count DESC
            """)
    List<TopRecipeProjection> findTopRecipesByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT s.recipeId)
            FROM MealPlanSlotEntity s
            JOIN s.plan p
            WHERE p.user.id = :userId AND s.status = 'EATEN' AND s.recipeId IS NOT NULL
            """)
    Integer countDistinctRecipesEatenByUserId(@Param("userId") Long userId);
}
