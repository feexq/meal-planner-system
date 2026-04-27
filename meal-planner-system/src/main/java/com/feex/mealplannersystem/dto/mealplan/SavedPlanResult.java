package com.feex.mealplannersystem.dto.mealplan;


import com.feex.mealplannersystem.mealplan.dto.plan.WeeklyMealPlanDto;
import lombok.Getter;

@Getter
public class SavedPlanResult {
    private final Long planId;
    private final FinalizedMealPlanDto finalizedPlan;
    private final WeeklyMealPlanDto rawPlan;

    public SavedPlanResult(Long planId, FinalizedMealPlanDto fp, WeeklyMealPlanDto rp) {
        this.planId = planId; this.finalizedPlan = fp; this.rawPlan = rp;
    }
}