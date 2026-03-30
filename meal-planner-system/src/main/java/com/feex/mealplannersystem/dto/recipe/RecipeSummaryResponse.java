package com.feex.mealplannersystem.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private String mealType;
    private String cookTime;
    private String cookComplexity;
    private String cookBudget;
    private Integer servings;
    private Set<String> tags;
    private BigDecimal calories;
}