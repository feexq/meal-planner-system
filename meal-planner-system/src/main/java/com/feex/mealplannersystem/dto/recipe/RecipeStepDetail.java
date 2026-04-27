package com.feex.mealplannersystem.dto.recipe;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecipeStepDetail {
    @JsonProperty("step_number")
    private Integer stepNumber;
    private String description;
}