package com.feex.mealplannersystem.mealplan.common;

public enum ConditionSeverity {
    CRITICAL, HIGH, MODERATE, MILD;

    public static ConditionSeverity of(String condition) {
        return switch (condition.toLowerCase()) {
            case "celiac_disease", "nut_allergy","shellfish_allergy", "fish_allergy" -> CRITICAL;
            case "diabetes", "kidney_disease" -> HIGH;
            case "hypertension", "high_cholesterol", "gout", "pancreatitis" -> MODERATE;
            default -> MILD;
        };
    }
}
