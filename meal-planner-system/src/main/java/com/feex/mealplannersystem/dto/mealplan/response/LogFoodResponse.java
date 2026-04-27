package com.feex.mealplannersystem.dto.mealplan.response;

import com.feex.mealplannersystem.dto.mealplan.ParsedFoodItem;
import com.feex.mealplannersystem.dto.mealplan.WeeklyBalanceDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LogFoodResponse {
    private List<ParsedFoodItem> parsedItems;
    private double totalCalories;
    private double totalProteinG;
    private double totalCarbsG;
    private double totalFatG;
    private String parseNote;
    private WeeklyBalanceDto weeklyBalance;
}
