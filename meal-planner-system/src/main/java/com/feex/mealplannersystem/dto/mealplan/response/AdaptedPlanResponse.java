package com.feex.mealplannersystem.dto.mealplan.response;

import com.feex.mealplannersystem.dto.mealplan.DayTargetDto;
import com.feex.mealplannersystem.dto.mealplan.WeeklyBalanceDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdaptedPlanResponse {
    private Long planId;
    private WeeklyBalanceDto weeklyBalance;
    private List<DayTargetDto> updatedDayTargets;
}