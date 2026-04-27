package com.feex.mealplannersystem.dto.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParsedFoodItem {
    private String name;
    private String original;
    @JsonProperty("quantity_description")
    private String quantityDescription;
    private double calories;
    @JsonProperty("protein_g")
    private double proteinG;
    @JsonProperty("carbs_g")
    private double carbsG;
    @JsonProperty("fat_g")
    private double fatG;
    private String confidence;
    @JsonProperty("from_cache")
    private boolean fromCache;
}