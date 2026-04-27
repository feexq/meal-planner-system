package com.feex.mealplannersystem.dto.mealplan;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DayTargetDto {
    private int dayNumber;
    private double originalTarget;
    private double adjustedTarget;
    private double delta;
    private String suggestedAction;
}

