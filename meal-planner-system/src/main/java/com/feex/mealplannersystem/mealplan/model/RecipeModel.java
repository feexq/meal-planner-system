package com.feex.mealplannersystem.mealplan.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class RecipeModel {

    private long id;
    private String name;
    private String description;
    private String mealType;
    private String cookTime;
    private String cookComplexity;
    private String cookBudget;
    private int servings;
    private String servingSize;
    private List<String> parsedIngredients;
    private List<String> tags;
}
