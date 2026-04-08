package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.mealplan.FoodLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLogEntity, Long> {

    List<FoodLogEntity> findByPlanIdAndDayNumber(Long planId, int dayNumber);

    @Query("""
            SELECT COALESCE(SUM(f.totalCalories), 0)
            FROM FoodLogEntity f
            WHERE f.plan.id = :planId AND f.dayNumber = :dayNumber
            """)
    Double sumExtraCaloriesByDay(
            @Param("planId") Long planId,
            @Param("dayNumber") int dayNumber);

    @Query("""
            SELECT COALESCE(SUM(f.totalCalories), 0)
            FROM FoodLogEntity f
            WHERE f.plan.id = :planId
            """)
    Double sumExtraCaloriesTotal(@Param("planId") Long planId);
}
