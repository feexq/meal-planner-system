package com.feex.mealplannersystem.config.normalizer.dto;


import lombok.Getter;

@Getter
public class ChosenItem {
    private Long recipeId;
    private String name;
    private double calories;
    private double proteinG;
    private double carbsG;
    private double fatG;
    private String reason;
}
