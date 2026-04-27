package com.feex.mealplannersystem.dto.dietary;

import com.feex.mealplannersystem.common.mealplan.DietaryConditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietaryConditionResponse {
    private String id;
    private String name;
    private String description;
    private DietaryConditionType type;
}