package com.feex.mealplannersystem.mealplan.service.calculator;

import lombok.Getter;

@Getter
public class MacroTarget {
    public final int proteinTargetG, proteinMinG, proteinMaxG;
    public final int fatTargetG, fatMinG, fatMaxG, saturatedFatMaxG;
    public final int carbsTargetG, carbsAbsoluteMinG;
    public final boolean lowCarbWarning;
    public final int proteinPerMealG, fatPerMealG, carbsPerMealG;
    public final int proteinPerBreakfastG, proteinPerLunchG, proteinPerDinnerG, proteinPerSnackG;
    public final int carbsPerBreakfastG, carbsPerLunchG, carbsPerDinnerG, carbsPerSnackG;
    public final boolean recommendSmallerPortions;

    public MacroTarget(int proteinTargetG, int proteinMinG, int proteinMaxG,
                       int fatTargetG, int fatMinG, int fatMaxG, int saturatedFatMaxG,
                       int carbsTargetG, int carbsAbsoluteMinG, boolean lowCarbWarning,
                       int mealsPerDay, boolean recommendSmallerPortions) {
        this.proteinTargetG = proteinTargetG;
        this.proteinMinG = proteinMinG;
        this.proteinMaxG = proteinMaxG;
        this.fatTargetG = fatTargetG;
        this.fatMinG = fatMinG;
        this.fatMaxG = fatMaxG;
        this.saturatedFatMaxG = saturatedFatMaxG;
        this.carbsTargetG = carbsTargetG;
        this.carbsAbsoluteMinG = carbsAbsoluteMinG;
        this.lowCarbWarning = lowCarbWarning;
        this.recommendSmallerPortions = recommendSmallerPortions;

        int meals = mealsPerDay > 0 ? mealsPerDay : 3;
        this.proteinPerMealG = (int) Math.round((double) proteinTargetG / meals);
        this.fatPerMealG     = (int) Math.round((double) fatTargetG / meals);
        this.carbsPerMealG   = (int) Math.round((double) carbsTargetG / meals);

        double[] pw = mealWeights(meals);
        this.proteinPerBreakfastG = (int) Math.round(proteinTargetG * pw[0]);
        this.proteinPerLunchG     = (int) Math.round(proteinTargetG * pw[1]);
        this.proteinPerDinnerG    = (int) Math.round(proteinTargetG * pw[2]);
        this.proteinPerSnackG     = meals >= 4 ? (int) Math.round(proteinTargetG * pw[3]) : 0;

        double[] cw = carbWeights(meals);
        this.carbsPerBreakfastG = (int) Math.round(carbsTargetG * cw[0]);
        this.carbsPerLunchG     = (int) Math.round(carbsTargetG * cw[1]);
        this.carbsPerDinnerG    = (int) Math.round(carbsTargetG * cw[2]);
        this.carbsPerSnackG     = meals >= 4 ? (int) Math.round(carbsTargetG * cw[3]) : 0;
    }

    public int getProteinTargetForSlot(String slotType) {
        if (slotType == null) return proteinPerMealG;
        return switch (slotType.toLowerCase()) {
            case "breakfast" -> proteinPerBreakfastG;
            case "lunch"     -> proteinPerLunchG;
            case "dinner"    -> proteinPerDinnerG;
            case "snack", "snack_2" -> proteinPerSnackG > 0 ? proteinPerSnackG : proteinPerMealG;
            default          -> proteinPerMealG;
        };
    }

    public int getCarbsTargetForSlot(String slotType) {
        if (slotType == null) return carbsPerMealG;
        return switch (slotType.toLowerCase()) {
            case "breakfast" -> carbsPerBreakfastG;
            case "lunch"     -> carbsPerLunchG;
            case "dinner"    -> carbsPerDinnerG;
            case "snack", "snack_2" -> carbsPerSnackG > 0 ? carbsPerSnackG : carbsPerMealG;
            default          -> carbsPerMealG;
        };
    }

    private static double[] mealWeights(int meals) {
        return switch (meals) {
            case 4  -> new double[]{0.20, 0.35, 0.30, 0.15};
            case 5  -> new double[]{0.18, 0.30, 0.28, 0.12, 0.12};
            default -> new double[]{0.25, 0.40, 0.35};
        };
    }

    private static double[] carbWeights(int meals) {
        return switch (meals) {
            case 4  -> new double[]{0.25, 0.35, 0.28, 0.12};
            case 5  -> new double[]{0.22, 0.30, 0.25, 0.12, 0.11};
            default -> new double[]{0.30, 0.40, 0.30};
        };
    }
}
