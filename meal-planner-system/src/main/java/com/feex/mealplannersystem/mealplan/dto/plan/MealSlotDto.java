package com.feex.mealplannersystem.mealplan.dto.plan;

import com.feex.mealplannersystem.mealplan.dto.scoring.RecipeCandidateDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MealSlotDto {
    private String mealType;
    private int slotCalorieBudget;
    private List<RecipeCandidateDto> candidates;
    private boolean lowCoverage;
    private String filteringNote;
}
