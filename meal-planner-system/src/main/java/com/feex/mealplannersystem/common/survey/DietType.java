package com.feex.mealplannersystem.common.survey;

public enum DietType {
    OMNIVORE,
    VEGETARIAN,
    VEGAN,
    KETO,
    PALEO,
    MEDITERRANEAN,
    LOW_CALORIE,
    GLUTEN_FREE;

    // Метод для зручного отримання ключа, який співпадає з вашою БД
    public String toDbSlug() {
        return this.name().toLowerCase();
    }
}