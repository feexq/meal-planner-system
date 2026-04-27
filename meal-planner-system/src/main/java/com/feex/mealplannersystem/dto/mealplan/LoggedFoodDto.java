package com.feex.mealplannersystem.dto.mealplan;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LoggedFoodDto {
    private Long logId;
    private String rawInput;
    private double totalCalories;
    private double proteinG;
    private double carbsG;
    private double fatG;
    private String confidence;
    private LocalDateTime loggedAt;
}
