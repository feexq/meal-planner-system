package com.feex.mealplannersystem.repository.projection;

public interface WeeklyAveragesProjection {
    Double getAvgCalories();
    Double getAvgProteinG();
    Double getAvgCarbsG();
    Double getAvgFatG();
    Double getAvgCompletionRate();
}