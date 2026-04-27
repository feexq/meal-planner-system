package com.feex.mealplannersystem.config.normalizer.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.feex.mealplannersystem.dto.mealplan.ParsedFoodItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ParseFoodResponse {
    private List<ParsedFoodItem> items;
    @JsonProperty("total_calories")
    private double totalCalories;
    @JsonProperty("total_protein_g")
    private double totalProteinG;
    @JsonProperty("total_carbs_g")
    private double totalCarbsG;
    @JsonProperty("total_fat_g")
    private double totalFatG;
    @JsonProperty("parse_note")
    private String parseNote;
}