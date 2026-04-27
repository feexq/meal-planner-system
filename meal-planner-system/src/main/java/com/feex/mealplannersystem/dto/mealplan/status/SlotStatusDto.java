package com.feex.mealplannersystem.dto.mealplan.status;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SlotStatusDto {
    private Long slotId;
    private String mealType;
    private Long recipeId;
    private String recipeName;
    private double targetCalories;
    private Double actualCalories;
    private double proteinG;
    private double fatG;
    private double carbsG;
    private String recipeSlug;
    private String slotRole;
    private String status;
    private LocalDateTime eatenAt;
}

