package com.feex.mealplannersystem.service.exception;

public class IngredientNotAvailableException extends RuntimeException {
    public static final String INGREDIENT_NOT_AVAILABLE_EXCEPTION = "Ingredient with id %s not available";

    public IngredientNotAvailableException(String ingredientId) { super(String.format(INGREDIENT_NOT_AVAILABLE_EXCEPTION, ingredientId)); }
}
