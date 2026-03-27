package service;

import model.UserProfile;
import java.util.List;
import java.util.ArrayList;

public class MacroRequirementCalculator {

    public static class MacroTarget {

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

        private static double[] getMealWeights(int meals) {
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

        private static double[] getCarbMealWeights(int meals) {
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

    public static MacroTarget calculateMacros(UserProfile user, double tdee) {

        String goal = user.getGoal() != null ? user.getGoal().toUpperCase() : "MAINTENANCE";
        String activity = user.getActivityLevel() != null ? user.getActivityLevel().toUpperCase() : "SEDENTARY";
        String diet = user.getDietType() != null ? user.getDietType().toUpperCase() : "OMNIVORE";
        int age = user.getAge();
        double weightKg = user.getWeightKg();
        int mealsPerDay = user.getMealsPerDay() > 0 ? user.getMealsPerDay() : 3;

        List<String> conditions = user.getHealthConditions() != null
                ? user.getHealthConditions()
                : new ArrayList<>();
        List<String> upperConditions = new ArrayList<>();
        for (String c : conditions)
            upperConditions.add(c.toUpperCase());

        boolean hasIbs = upperConditions.contains("IBS");

        boolean recommendSmallerPortions = hasIbs;

        double proteinLo;
        double proteinHi;

        switch (goal) {
            case "WEIGHT_LOSS":
                if ("ACTIVE".equals(activity) || "VERY_ACTIVE".equals(activity)) {
                    proteinLo = 1.6;
                    proteinHi = 2.4;
                } else if ("MODERATE".equals(activity)) {
                    proteinLo = 1.6;
                    proteinHi = 2.0;
                } else {
                    proteinLo = 1.2;
                    proteinHi = 1.5;
                }
                break;
            case "WEIGHT_GAIN":
                proteinLo = 1.6;
                proteinHi = 2.4;
                break;
            default: 
                if ("SEDENTARY".equals(activity)) {
                    proteinLo = 1.0;
                    proteinHi = 1.2;
                } else if ("LIGHT".equals(activity)) {
                    proteinLo = 1.2;
                    proteinHi = 1.6;
                } else if ("MODERATE".equals(activity) || "ACTIVE".equals(activity)) {
                    proteinLo = 1.4;
                    proteinHi = 2.0;
                } else {
                    proteinLo = 1.6;
                    proteinHi = 2.2;
                }
                break;
        }

        if (age >= 50) {
            proteinLo = Math.max(proteinLo, 1.5);
            proteinHi = Math.max(proteinHi, 2.2);
            if ("WEIGHT_GAIN".equals(goal))
                proteinLo = Math.max(proteinLo, 1.7);
        }

        double multiplier = 1.0;
        if ("VEGETARIAN".equals(diet))
            multiplier = 1.15;
        else if ("VEGAN".equals(diet) || "PLANT_BASED".equals(diet))
            multiplier = 1.20;
        proteinLo *= multiplier;
        proteinHi *= multiplier;

        int proteinTargetG = (int) Math.round(weightKg * ((proteinLo + proteinHi) / 2.0));
        int proteinMinG = (int) Math.round(weightKg * proteinLo);
        int proteinMaxG = (int) Math.round(weightKg * proteinHi);

        double effectiveTdee = Math.max(tdee, 800.0);

        int maxProteinCals = (int) (effectiveTdee * 0.50);
        if (proteinTargetG * 4 > maxProteinCals) {
            proteinTargetG = Math.max(maxProteinCals / 4, 1); 
        }

        proteinMinG = Math.min(proteinMinG, proteinTargetG);
        proteinMaxG = Math.max(proteinMaxG, proteinTargetG);

        int proteinCalories = proteinTargetG * 4;
        int remainingCals = (int) Math.round(effectiveTdee) - proteinCalories;

        remainingCals = Math.max(remainingCals, 0);

        int fatTargetG;
        int carbsTargetG;

        if (upperConditions.contains("DIABETES")) {
            int carbsCals = (int) Math.round(effectiveTdee * 0.40);
            carbsCals = Math.min(carbsCals, remainingCals);
            carbsTargetG = carbsCals / 4;
            int fatCals = remainingCals - (carbsTargetG * 4);
            fatTargetG = Math.max(fatCals / 9, 0);

        } else {
            double fatPct = "WEIGHT_LOSS".equals(goal) ? 0.25 : 0.30;
            if (upperConditions.contains("HIGH_CHOLESTEROL"))
                fatPct = Math.min(fatPct, 0.25);

            if (hasIbs)
                fatPct = Math.min(fatPct, 0.25);

            int fatCals = (int) Math.round(effectiveTdee * fatPct);
            fatCals = Math.min(fatCals, remainingCals);
            fatTargetG = fatCals / 9;

            int carbsCals = remainingCals - (fatTargetG * 9);
            carbsTargetG = Math.max(carbsCals / 4, 0);
        }

        final int carbsAbsoluteMinG = 130;
        boolean lowCarbWarning = carbsTargetG < carbsAbsoluteMinG;

        if (!upperConditions.contains("DIABETES") && carbsTargetG < carbsAbsoluteMinG) {
            int missingCarbsG = carbsAbsoluteMinG - carbsTargetG;
            int caloriesToSteal = missingCarbsG * 4;
            int stealableFromFat = fatTargetG * 9;
            if (stealableFromFat >= caloriesToSteal) {
                fatTargetG -= caloriesToSteal / 9;
                carbsTargetG = carbsAbsoluteMinG;
                lowCarbWarning = false;
            }
        }

        int fatMinG = (int) Math.round((effectiveTdee * 0.20) / 9.0);
        int fatMaxG = (int) Math.round((effectiveTdee * 0.35) / 9.0);
        fatTargetG = Math.max(fatTargetG, fatMinG);

        int saturatedFatMaxG = upperConditions.contains("HIGH_CHOLESTEROL")
                ? (int) Math.round((effectiveTdee * 0.07) / 9.0)
                : (int) Math.round((effectiveTdee * 0.10) / 9.0);

        return new MacroTarget(
                proteinTargetG, proteinMinG, proteinMaxG,
                fatTargetG, fatMinG, fatMaxG, saturatedFatMaxG,
                carbsTargetG, carbsAbsoluteMinG, lowCarbWarning,
                mealsPerDay,
                recommendSmallerPortions); 
    }

    public static int calcCarbsFit(double recipeCarbsG, int carbsPerMealG,
            int dailyCarbsTargetG, String recipeName) {

        if (dailyCarbsTargetG > 150 && recipeCarbsG < 10) {
            String nameLower = recipeName != null ? recipeName.toLowerCase() : "";
            if (nameLower.contains("low carb") || nameLower.contains("low-carb")
                    || nameLower.contains("keto") || nameLower.contains("zero carb")) {
                return 0;
            }
        }

        if (carbsPerMealG <= 0)
            return 5;

        double ratio = recipeCarbsG / (double) carbsPerMealG;
        if (ratio >= 0.85)
            return 10;
        else if (ratio >= 0.60)
            return 7;
        else if (ratio >= 0.35)
            return 4;
        else if (ratio >= 0.15)
            return 2;
        else
            return 0;
    }

    public static int calcFatFit(double recipeFatG, int fatPerMealG) {
        if (fatPerMealG <= 0)
            return 3;
        double ratio = recipeFatG / (double) fatPerMealG;
        if (ratio >= 0.50 && ratio <= 1.50)
            return 5;
        else if (ratio >= 0.30 && ratio <= 1.75)
            return 3;
        else if (ratio > 1.75)
            return 0;
        else
            return 1;
    }

    public static String calcCarbsStatus(int actualCarbsG, int carbsTargetG) {
        if (carbsTargetG <= 0)
            return "OK";
        double pct = (double) actualCarbsG / carbsTargetG * 100.0;
        if (pct < 40)
            return "CRITICAL";
        else if (pct < 70)
            return "LOW";
        else if (pct <= 130)
            return "OK";
        else
            return "HIGH";
    }

    public static String calcFatStatus(int actualFatG, int fatMinG, int fatMaxG) {
        if (actualFatG < fatMinG)
            return "LOW";
        else if (actualFatG > fatMaxG)
            return "HIGH";
        else
            return "OK";
    }
}