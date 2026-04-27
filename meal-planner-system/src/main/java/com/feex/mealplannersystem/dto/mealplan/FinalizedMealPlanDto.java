package com.feex.mealplannersystem.dto.mealplan;

import com.feex.mealplannersystem.mealplan.dto.finalize.FinalizedDayDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FinalizedMealPlanDto {
    private String userId;
    private List<FinalizedDayDto> days;
}