package com.feex.mealplannersystem.config.normalizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChosenItem {
    private Long recipeId;
    private String name;
    private Double calories;
    private Double proteinG;
    private Double carbsG;
    private Double fatG;
    private List<String> dietaryNotes;
}
