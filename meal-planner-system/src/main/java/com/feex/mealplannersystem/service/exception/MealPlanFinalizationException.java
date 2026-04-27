package com.feex.mealplannersystem.service.exception;

public class MealPlanFinalizationException extends RuntimeException {
    public static final String MEAL_PLAN_FINALIZATION_EXCEPTION_MESSAGE = "Meal plan finalization exception happened: %s, cause: %s";

    public MealPlanFinalizationException(Exception e) { super(String.format(MEAL_PLAN_FINALIZATION_EXCEPTION_MESSAGE, e.getMessage(), e)); }
}
