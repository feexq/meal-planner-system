package com.feex.mealplannersystem.dto.mealplan.status;

import com.feex.mealplannersystem.dto.mealplan.LoggedFoodDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DayStatusDto {
    private int dayNumber;
    private double targetCalories;
    private double plannedCalories;
    private double consumedCalories;
    private double extraCalories;
    private List<SlotStatusDto> slots;
    private List<LoggedFoodDto> extraFood;
}
