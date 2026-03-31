package com.feex.mealplannersystem.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;


@Getter
@Builder
@AllArgsConstructor
public class FilterDebugDto {
    private String userId;
    private String slotType;
    private Map<String, Integer> eliminations;
    private int validRecipes;
    private int totalRecipes;
    private String topEliminationCause;
}
