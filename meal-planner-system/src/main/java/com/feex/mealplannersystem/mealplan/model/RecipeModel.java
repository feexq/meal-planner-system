package com.feex.mealplannersystem.mealplan.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

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

    /** Ingredient names in lowercase, already parsed from RecipeIngredientEntity */
    private List<String> parsedIngredients;

    /** Tag names in lowercase */
    private List<String> tags;
}
