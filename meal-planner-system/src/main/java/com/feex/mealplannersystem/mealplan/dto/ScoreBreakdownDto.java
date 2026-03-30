package com.feex.mealplannersystem.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScoreBreakdownDto {
    private int calorieFit;
    private int complexityMatch;
    private int budgetMatch;
    private int cookTime;
    private int proteinFit;
    private int carbsFit;
    private int fatFit;
    private int nutritionQuality;
    private int variety;
    private int vegProteinBonus;
    private int softForbiddenPenalty;

    public int total() {
        return calorieFit + complexityMatch + budgetMatch + cookTime
                + proteinFit + carbsFit + fatFit + nutritionQuality
                + variety + vegProteinBonus - softForbiddenPenalty;
    }
}
