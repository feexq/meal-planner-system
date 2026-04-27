package com.feex.mealplannersystem.dto.recipe;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecipeIngredientDetail {
    private Double amount;
    private String unit;

    @JsonProperty("name_uk")
    private String nameUk;
    private String note;
}