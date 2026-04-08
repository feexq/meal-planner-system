package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
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
}
