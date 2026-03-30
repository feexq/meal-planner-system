package com.feex.mealplannersystem.domain.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdown {
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
                + proteinFit + carbsFit + fatFit + nutritionQuality + variety + vegProteinBonus - softForbiddenPenalty;
    }
}
