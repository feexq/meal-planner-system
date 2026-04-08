package com.feex.mealplannersystem.dto.mealplan;

import com.feex.mealplannersystem.mealplan.dto.FinalizedMealPlanDtos;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GenerateFinalResponse {
    private Long planId;
    private FinalizedMealPlanDtos.FinalizedMealPlanDto finalizedPlan;
}
