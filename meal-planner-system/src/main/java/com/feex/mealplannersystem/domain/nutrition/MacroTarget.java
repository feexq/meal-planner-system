package com.feex.mealplannersystem.domain.nutrition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
public class MacroTarget {
    public final int proteinTargetG;
    public final int proteinMinG;
    public final int proteinMaxG;

    public final int fatTargetG;
    public final int fatMinG;
    public final int fatMaxG;
    public final int saturatedFatMaxG;

    public final int carbsTargetG;
    public final int carbsAbsoluteMinG;
    public final boolean lowCarbWarning;

    public final int proteinPerMealG;
    public final int fatPerMealG;
    public final int carbsPerMealG;

    public final int proteinPerBreakfastG;
    public final int proteinPerLunchG;
    public final int proteinPerDinnerG;
    public final int proteinPerSnackG;

    public final int carbsPerBreakfastG;
    public final int carbsPerLunchG;
    public final int carbsPerDinnerG;
    public final int carbsPerSnackG;

    public final boolean recommendSmallerPortions;

    public MacroTarget(
            int proteinTargetG, int proteinMinG, int proteinMaxG,
            int fatTargetG, int fatMinG, int fatMaxG, int saturatedFatMaxG,
            int carbsTargetG, int carbsAbsoluteMinG, boolean lowCarbWarning,
            int mealsPerDay,
            boolean recommendSmallerPortions) {
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
        this.fatPerMealG = (int) Math.round((double) fatTargetG / meals);
        this.carbsPerMealG = (int) Math.round((double) carbsTargetG / meals);

        double[] weights = getMealWeights(meals);
        this.proteinPerBreakfastG = (int) Math.round(proteinTargetG * weights[0]);
        this.proteinPerLunchG = (int) Math.round(proteinTargetG * weights[1]);
        this.proteinPerDinnerG = (int) Math.round(proteinTargetG * weights[2]);
        this.proteinPerSnackG = meals >= 4
                ? (int) Math.round(proteinTargetG * weights[3])
                : 0;

        double[] carbWeights = getCarbMealWeights(meals);
        this.carbsPerBreakfastG = (int) Math.round(carbsTargetG * carbWeights[0]);
        this.carbsPerLunchG = (int) Math.round(carbsTargetG * carbWeights[1]);
        this.carbsPerDinnerG = (int) Math.round(carbsTargetG * carbWeights[2]);
        this.carbsPerSnackG = meals >= 4
                ? (int) Math.round(carbsTargetG * carbWeights[3])
                : 0;
    }

    public MacroTarget(
            int proteinTargetG, int proteinMinG, int proteinMaxG,
            int fatTargetG, int fatMinG, int fatMaxG, int saturatedFatMaxG,
            int carbsTargetG, int carbsAbsoluteMinG, boolean lowCarbWarning,
            int mealsPerDay) {
        this(proteinTargetG, proteinMinG, proteinMaxG,
                fatTargetG, fatMinG, fatMaxG, saturatedFatMaxG,
                carbsTargetG, carbsAbsoluteMinG, lowCarbWarning,
                mealsPerDay, false);
    }

    public int getProteinTargetForSlot(String slotType) {
        if (slotType == null)
            return proteinPerMealG;
        switch (slotType.toLowerCase()) {
            case "breakfast":
                return proteinPerBreakfastG;
            case "lunch":
                return proteinPerLunchG;
            case "dinner":
                return proteinPerDinnerG;
            case "snack":
            case "snack_2":
                return proteinPerSnackG > 0 ? proteinPerSnackG : proteinPerMealG;
            default:
                return proteinPerMealG;
        }
    }

    public int getCarbsTargetForSlot(String slotType) {
        if (slotType == null)
            return carbsPerMealG;
        switch (slotType.toLowerCase()) {
            case "breakfast":
                return carbsPerBreakfastG;
            case "lunch":
                return carbsPerLunchG;
            case "dinner":
                return carbsPerDinnerG;
            case "snack":
            case "snack_2":
                return carbsPerSnackG > 0 ? carbsPerSnackG : carbsPerMealG;
            default:
                return carbsPerMealG;
        }
    }

    private double[] getMealWeights(int meals) {
        switch (meals) {
            case 3:
                return new double[] { 0.25, 0.40, 0.35 };
            case 4:
                return new double[] { 0.20, 0.35, 0.30, 0.15 };
            case 5:
                return new double[] { 0.18, 0.30, 0.28, 0.12, 0.12 };
            default:
                return new double[] { 0.25, 0.40, 0.35 };
        }
    }

    private double[] getCarbMealWeights(int meals) {
        switch (meals) {
            case 3:
                return new double[] { 0.30, 0.40, 0.30 };
            case 4:
                return new double[] { 0.25, 0.35, 0.28, 0.12 };
            case 5:
                return new double[] { 0.22, 0.30, 0.25, 0.12, 0.11 };
            default:
                return new double[] { 0.30, 0.40, 0.30 };
        }
    }
}
