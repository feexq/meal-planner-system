package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MealPlanRecordRepository extends JpaRepository<MealPlanRecordEntity, Long> {

    @Query("""
            SELECT p FROM MealPlanRecordEntity p
            LEFT JOIN FETCH p.slots
            LEFT JOIN FETCH p.foodLogs
            WHERE p.user.id = :userId AND p.status = 'ACTIVE'
            ORDER BY p.createdAt DESC
            """)
    Optional<MealPlanRecordEntity> findActiveByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT p FROM MealPlanRecordEntity p
            LEFT JOIN FETCH p.slots
            LEFT JOIN FETCH p.foodLogs
            WHERE p.id = :planId AND p.user.id = :userId
            """)
    Optional<MealPlanRecordEntity> findByIdAndUserId(
            @Param("planId") Long planId,
            @Param("userId") Long userId);
}
