package com.feex.mealplannersystem.mealplan.dto.finalize;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FinalizedDayDto {
    private int day;
    private int dailyCalorieTarget;
    private List<FinalizedSlotDto> slots;
    private double dayTotalCalories;
    private double dailyProteinG;
    private double dailyCarbsG;
    private double dailyFatG;
    private String notes;
}
