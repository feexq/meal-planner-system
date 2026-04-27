package com.feex.mealplannersystem.dto.mealplan.response;

import com.feex.mealplannersystem.dto.mealplan.FinalizedMealPlanDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GenerateFinalResponse {
    private Long planId;
    private FinalizedMealPlanDto finalizedPlan;
}
