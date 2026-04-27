package com.feex.mealplannersystem.dto.mealplan.status;

import com.feex.mealplannersystem.dto.mealplan.DayTargetDto;
import com.feex.mealplannersystem.dto.mealplan.WeeklyBalanceDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlanStatusDto {
    private Long planId;
    private String weekStartDate;
    private WeeklyBalanceDto weeklyBalance;
    private List<DayTargetDto> updatedTargets;
    private List<DayStatusDto> days;
}