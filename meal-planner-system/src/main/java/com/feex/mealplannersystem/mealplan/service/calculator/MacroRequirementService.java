package com.feex.mealplannersystem.mealplan.service.calculator;

import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring component wrapping prototype MacroRequirementCalculator.
 * calculateMacros() is the main entry; all inner logic is identical to the prototype.
 */
@Component
public class MacroRequirementService {

    @Getter
    public static class MacroTarget {
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

    // -----------------------------------------------------------------------
    // Main calculation — logic identical to prototype
    // -----------------------------------------------------------------------

    public MacroTarget calculateMacros(UserProfileModel user, double tdee) {
        String goal     = upper(user.getGoal(), "MAINTENANCE");
        String activity = upper(user.getActivityLevel(), "SEDENTARY");
        String diet     = upper(user.getDietType(), "OMNIVORE");
        int age         = user.getAge();
        double weightKg = user.getWeightKg();
        int meals       = user.getMealsPerDay() > 0 ? user.getMealsPerDay() : 3;

        List<String> conditions = new ArrayList<>();
        if (user.getHealthConditions() != null)
            user.getHealthConditions().forEach(c -> conditions.add(c.toUpperCase()));

        boolean hasIbs = conditions.contains("IBS");

        double proteinLo, proteinHi;
        switch (goal) {
            case "WEIGHT_LOSS" -> {
                if ("ACTIVE".equals(activity) || "VERY_ACTIVE".equals(activity)) { proteinLo = 1.6; proteinHi = 2.4; }
                else if ("MODERATE".equals(activity))                             { proteinLo = 1.6; proteinHi = 2.0; }
                else                                                              { proteinLo = 1.2; proteinHi = 1.5; }
            }
            case "WEIGHT_GAIN" -> { proteinLo = 1.6; proteinHi = 2.4; }
            default -> {
                if ("SEDENTARY".equals(activity))                                                { proteinLo = 1.0; proteinHi = 1.2; }
                else if ("LIGHT".equals(activity))                                               { proteinLo = 1.2; proteinHi = 1.6; }
                else if ("MODERATE".equals(activity) || "ACTIVE".equals(activity))              { proteinLo = 1.4; proteinHi = 2.0; }
                else                                                                             { proteinLo = 1.6; proteinHi = 2.2; }
            }
        }
        if (age >= 50) {
            proteinLo = Math.max(proteinLo, 1.5);
            proteinHi = Math.max(proteinHi, 2.2);
            if ("WEIGHT_GAIN".equals(goal)) proteinLo = Math.max(proteinLo, 1.7);
        }
        double multiplier = switch (diet) {
            case "VEGETARIAN"             -> 1.15;
            case "VEGAN", "PLANT_BASED"   -> 1.20;
            default                       -> 1.0;
        };
        proteinLo *= multiplier;
        proteinHi *= multiplier;

        double effectiveTdee = Math.max(tdee, 800.0);
        int proteinTargetG = (int) Math.round(weightKg * ((proteinLo + proteinHi) / 2.0));
        int proteinMinG    = (int) Math.round(weightKg * proteinLo);
        int proteinMaxG    = (int) Math.round(weightKg * proteinHi);
        int maxProteinCals = (int) (effectiveTdee * 0.50);
        if (proteinTargetG * 4 > maxProteinCals) proteinTargetG = Math.max(maxProteinCals / 4, 1);
        proteinMinG = Math.min(proteinMinG, proteinTargetG);
        proteinMaxG = Math.max(proteinMaxG, proteinTargetG);

        int proteinCalories = proteinTargetG * 4;
        int remaining = Math.max((int) Math.round(effectiveTdee) - proteinCalories, 0);

        int fatTargetG, carbsTargetG;
        if (conditions.contains("DIABETES")) {
            int carbsCals = Math.min((int) Math.round(effectiveTdee * 0.40), remaining);
            carbsTargetG = carbsCals / 4;
            fatTargetG   = Math.max((remaining - carbsTargetG * 4) / 9, 0);
        } else {
            double fatPct = "WEIGHT_LOSS".equals(goal) ? 0.25 : 0.30;
            if (conditions.contains("HIGH_CHOLESTEROL")) fatPct = Math.min(fatPct, 0.25);
            if (hasIbs) fatPct = Math.min(fatPct, 0.25);
            int fatCals   = Math.min((int) Math.round(effectiveTdee * fatPct), remaining);
            fatTargetG    = fatCals / 9;
            carbsTargetG  = Math.max((remaining - fatTargetG * 9) / 4, 0);
        }

        final int carbsAbsoluteMinG = 130;
        boolean lowCarbWarning = carbsTargetG < carbsAbsoluteMinG;
        if (!conditions.contains("DIABETES") && carbsTargetG < carbsAbsoluteMinG) {
            int missing = carbsAbsoluteMinG - carbsTargetG;
            int steal   = missing * 4;
            if (fatTargetG * 9 >= steal) {
                fatTargetG  -= steal / 9;
                carbsTargetG = carbsAbsoluteMinG;
                lowCarbWarning = false;
            }
        }

        int fatMinG          = (int) Math.round((effectiveTdee * 0.20) / 9.0);
        int fatMaxG          = (int) Math.round((effectiveTdee * 0.35) / 9.0);
        fatTargetG           = Math.max(fatTargetG, fatMinG);
        int saturatedFatMaxG = conditions.contains("HIGH_CHOLESTEROL")
                ? (int) Math.round((effectiveTdee * 0.07) / 9.0)
                : (int) Math.round((effectiveTdee * 0.10) / 9.0);

        return new MacroTarget(
                proteinTargetG, proteinMinG, proteinMaxG,
                fatTargetG, fatMinG, fatMaxG, saturatedFatMaxG,
                carbsTargetG, carbsAbsoluteMinG, lowCarbWarning,
                meals, hasIbs);
    }

    // -----------------------------------------------------------------------
    // Static helpers used by RecipeScorerService
    // -----------------------------------------------------------------------

    public static int calcCarbsFit(double recipeCarbsG, int carbsPerMealG, int dailyCarbsTargetG, String recipeName) {
        if (dailyCarbsTargetG > 150 && recipeCarbsG < 10) {
            String nl = recipeName != null ? recipeName.toLowerCase() : "";
            if (nl.contains("low carb") || nl.contains("keto") || nl.contains("zero carb")) return 0;
        }
        if (carbsPerMealG <= 0) return 5;
        double ratio = recipeCarbsG / (double) carbsPerMealG;
        if (ratio >= 0.85) return 10;
        if (ratio >= 0.60) return 7;
        if (ratio >= 0.35) return 4;
        if (ratio >= 0.15) return 2;
        return 0;
    }

    public static int calcFatFit(double recipeFatG, int fatPerMealG) {
        if (fatPerMealG <= 0) return 3;
        double ratio = recipeFatG / (double) fatPerMealG;
        if (ratio >= 0.50 && ratio <= 1.50) return 5;
        if (ratio >= 0.30 && ratio <= 1.75) return 3;
        if (ratio > 1.75) return 0;
        return 1;
    }

    public static String calcCarbsStatus(int actualCarbsG, int carbsTargetG) {
        if (carbsTargetG <= 0) return "OK";
        double pct = (double) actualCarbsG / carbsTargetG * 100.0;
        if (pct < 40) return "CRITICAL";
        if (pct < 70) return "LOW";
        if (pct <= 130) return "OK";
        return "HIGH";
    }

    public static String calcFatStatus(int actualFatG, int fatMinG, int fatMaxG) {
        if (actualFatG < fatMinG) return "LOW";
        if (actualFatG > fatMaxG) return "HIGH";
        return "OK";
    }

    private static String upper(String v, String def) {
        return v != null ? v.toUpperCase() : def;
    }
}
