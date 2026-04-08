package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.repository.projection.WeeklyAveragesProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyNutritionSummaryRepository extends JpaRepository<DailyNutritionSummaryEntity, Long> {

    Optional<DailyNutritionSummaryEntity> findByUserIdAndSummaryDate(Long userId, LocalDate summaryDate);

    @Query("SELECT d FROM DailyNutritionSummaryEntity d WHERE d.user.id = :userId " +
            "AND d.summaryDate BETWEEN :from AND :to " +
            "ORDER BY d.summaryDate ASC")
    List<DailyNutritionSummaryEntity> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
    SELECT 
        AVG(d.totalCalories) AS avgCalories,
        AVG(d.totalProteinG) AS avgProteinG,
        AVG(d.totalCarbsG) AS avgCarbsG,
        AVG(d.totalFatG) AS avgFatG,
        AVG(d.completionRate) AS avgCompletionRate
    FROM DailyNutritionSummaryEntity d 
    WHERE d.user.id = :userId 
      AND d.summaryDate BETWEEN :startDate AND :endDate
    """)
    WeeklyAveragesProjection getAveragesByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(d) FROM DailyNutritionSummaryEntity d " +
            "WHERE d.user.id = :userId " +
            "AND d.summaryDate BETWEEN :from AND :to " +
            "AND d.completionRate >= :threshold")
    int countCompletedDays(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("threshold") int threshold
    );
}