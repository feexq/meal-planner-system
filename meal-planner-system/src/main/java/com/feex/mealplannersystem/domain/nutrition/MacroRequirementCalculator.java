package com.feex.mealplannersystem.domain.nutrition;

import com.feex.mealplannersystem.common.survey.ActivityLevel;
import com.feex.mealplannersystem.common.survey.DietType;
import com.feex.mealplannersystem.common.survey.Goal;
import com.feex.mealplannersystem.repository.entity.preference.UserHealthConditionEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MacroRequirementCalculator {
    public MacroTarget calculateMacros(UserPreferenceEntity user, double tdee) {

        Goal goal = user.getGoal() != null ? user.getGoal() : Goal.MAINTENANCE;
        ActivityLevel activity = user.getActivityLevel() != null ? user.getActivityLevel() : ActivityLevel.SEDENTARY;
        DietType diet = user.getDietType() != null ? user.getDietType() : DietType.OMNIVORE;
        int age = user.getAge();
        double weightKg = user.getWeightKg();
        int mealsPerDay = user.getMealsPerDay() > 0 ? user.getMealsPerDay() : 3;


        List<String> conditions = user.getHealthConditions() != null
                ? user.getHealthConditions().stream()
                    .map(UserHealthConditionEntity::getConditionName)
                    .toList()
                : new ArrayList<>();
        List<String> upperConditions = new ArrayList<>();
        for (String c : conditions) {
            upperConditions.add(c.toUpperCase());
        }

        boolean hasIbs = upperConditions.contains("IBS");
        boolean recommendSmallerPortions = hasIbs;

        double proteinLo;
        double proteinHi;

        switch (goal) {
            case WEIGHT_LOSS:
                if (ActivityLevel.ACTIVE.equals(activity) || ActivityLevel.VERY_ACTIVE.equals(activity)) {
                    proteinLo = 1.6;
                    proteinHi = 2.4;
                } else if (ActivityLevel.MODERATE.equals(activity)) {
                    proteinLo = 1.6;
                    proteinHi = 2.0;
                } else {
                    proteinLo = 1.2;
                    proteinHi = 1.5;
                }
                break;
            case WEIGHT_GAIN:
                proteinLo = 1.6;
                proteinHi = 2.4;
                break;
            default:
                if (ActivityLevel.SEDENTARY.equals(activity)) {
                    proteinLo = 1.0;
                    proteinHi = 1.2;
                } else if (ActivityLevel.LIGHT.equals(activity)) {
                    proteinLo = 1.2;
                    proteinHi = 1.6;
                } else if (ActivityLevel.MODERATE.equals(activity) || ActivityLevel.ACTIVE.equals(activity)) {
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
            if (Goal.WEIGHT_GAIN.equals(goal))
                proteinLo = Math.max(proteinLo, 1.7);
        }

        double multiplier = 1.0;
        if (DietType.VEGETARIAN.equals(diet))
            multiplier = 1.15;
        else if (DietType.VEGAN.equals(diet))
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
            double fatPct = Goal.WEIGHT_LOSS.equals(goal) ? 0.25 : 0.30;
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

    public int calcCarbsFit(double recipeCarbsG, int carbsPerMealG,
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

    public int calcFatFit(double recipeFatG, int fatPerMealG) {
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

    public String calcCarbsStatus(int actualCarbsG, int carbsTargetG) {
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

    public String calcFatStatus(int actualFatG, int fatMinG, int fatMaxG) {
        if (actualFatG < fatMinG)
            return "LOW";
        else if (actualFatG > fatMaxG)
            return "HIGH";
        else
            return "OK";
    }
}
