package model.output;

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

    public ScoreBreakdown(int calorieFit, int complexityMatch, int budgetMatch,
            int cookTime, int proteinFit, int carbsFit, int fatFit, int nutritionQuality, int variety,
            int vegProteinBonus, int softForbiddenPenalty) {
        this.calorieFit = calorieFit;
        this.complexityMatch = complexityMatch;
        this.budgetMatch = budgetMatch;
        this.cookTime = cookTime;
        this.proteinFit = proteinFit;
        this.carbsFit = carbsFit;
        this.fatFit = fatFit;
        this.nutritionQuality = nutritionQuality;
        this.variety = variety;
        this.vegProteinBonus = vegProteinBonus;
        this.softForbiddenPenalty = softForbiddenPenalty;
    }

    public int getCalorieFit() {
        return calorieFit;
    }

    public int getComplexityMatch() {
        return complexityMatch;
    }

    public int getBudgetMatch() {
        return budgetMatch;
    }

    public int getCookTime() {
        return cookTime;
    }

    public int getProteinFit() {
        return proteinFit;
    }

    public int getNutritionQuality() {
        return nutritionQuality;
    }

    public int getVariety() {
        return variety;
    }

    public int getVegProteinBonus() {
        return vegProteinBonus;
    }

    public int getSoftForbiddenPenalty() {
        return softForbiddenPenalty;
    }

    public int getCarbsFit() {
        return carbsFit;
    }

    public int getFatFit() {
        return fatFit;
    }

    public int total() {
        return calorieFit + complexityMatch + budgetMatch + cookTime
                + proteinFit + carbsFit + fatFit + nutritionQuality + variety + vegProteinBonus - softForbiddenPenalty;
    }
}
