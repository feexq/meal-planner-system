package com.feex.mealplannersystem.dto.dietary;

import com.feex.mealplannersystem.common.DietaryConditionType;
import com.feex.mealplannersystem.common.DietaryTagStatus;
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