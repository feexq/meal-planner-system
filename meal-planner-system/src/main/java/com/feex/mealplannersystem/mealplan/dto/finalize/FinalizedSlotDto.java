package com.feex.mealplannersystem.mealplan.dto.finalize;

import com.feex.mealplannersystem.mealplan.dto.plan.MealItemDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FinalizedSlotDto {
    private String mealType;
    private int calorieBudget;
    private MealItemDto main;
    private MealItemDto side;
    private double slotTotalCalories;
    private double slotProteinG;
    private double slotCarbsG;
    private double slotFatG;
}
