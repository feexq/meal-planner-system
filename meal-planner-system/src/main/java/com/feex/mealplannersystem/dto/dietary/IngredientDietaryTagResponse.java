package com.feex.mealplannersystem.dto.dietary;

import com.feex.mealplannersystem.common.mealplan.DietaryConditionType;
import com.feex.mealplannersystem.common.mealplan.DietaryTagStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDietaryTagResponse {
    private String conditionId;
    private String conditionName;
    private DietaryConditionType conditionType;
    private DietaryTagStatus status;
}