package com.feex.mealplannersystem.mealplan.service.calculator;

import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MacroRequirementService {

    public MacroTarget calculateMacros(UserProfileModel user, double tdee) {
        double effectiveTdee = Math.max(tdee, 800.0);
        List<String> conditions = extractConditions(user);

        double[] proteinRange = resolveProteinRange(user, conditions);
        int[] proteinTargets = calculateProteinTargets(user, effectiveTdee, proteinRange);
        int[] fatAndCarbs = calculateFatAndCarbs(user, effectiveTdee, conditions, proteinTargets[0]);
        fatAndCarbs = adjustCarbsMinimum(fatAndCarbs, conditions);

        return buildMacroTarget(proteinTargets, fatAndCarbs, effectiveTdee, conditions, user.getMealsPerDay());
    }

    private List<String> extractConditions(UserProfileModel user) {
        if (user.getHealthConditions() == null) return List.of();
        return user.getHealthConditions().stream()
                .map(String::toUpperCase)
                .toList();
    }

    private double[] resolveProteinRange(UserProfileModel user, List<String> conditions) {
        String goal = upper(user.getGoal(), "MAINTENANCE");
        String activity = upper(user.getActivityLevel(), "SEDENTARY");
        String diet = upper(user.getDietType(), "OMNIVORE");

        double lo, hi;
        switch (goal) {
            case "WEIGHT_LOSS" -> {
                if ("ACTIVE".equals(activity) || "VERY_ACTIVE".equals(activity)) { lo = 1.6; hi = 2.4; }
                else if ("MODERATE".equals(activity)) { lo = 1.6; hi = 2.0; }
                else { lo = 1.2; hi = 1.5; }
            }
            case "WEIGHT_GAIN" -> { lo = 1.6; hi = 2.4; }
            default -> {
                if ("SEDENTARY".equals(activity)) { lo = 1.0; hi = 1.2; }
                else if ("LIGHT".equals(activity)) { lo = 1.2; hi = 1.6; }
                else if ("MODERATE".equals(activity) || "ACTIVE".equals(activity)) { lo = 1.4; hi = 2.0; }
                else { lo = 1.6; hi = 2.2; }
            }
        }

        if (user.getAge() >= 50) {
            lo = Math.max(lo, 1.5);
            hi = Math.max(hi, 2.2);
            if ("WEIGHT_GAIN".equals(goal)) lo = Math.max(lo, 1.7);
        }

        double multiplier = switch (diet) {
            case "VEGETARIAN" -> 1.15;
            case "VEGAN", "PLANT_BASED" -> 1.20;
            default -> 1.0;
        };

        return new double[]{ lo * multiplier, hi * multiplier };
    }

    private int[] calculateProteinTargets(UserProfileModel user, double tdee, double[] range) {
        double lo = range[0], hi = range[1];
        double weightKg = user.getWeightKg();

        int target = (int) Math.round(weightKg * ((lo + hi) / 2.0));
        int min = (int) Math.round(weightKg * lo);
        int max = (int) Math.round(weightKg * hi);

        int maxProteinCals = (int) (tdee * 0.50);
        if (target * 4 > maxProteinCals) target = Math.max(maxProteinCals / 4, 1);
        min = Math.min(min, target);
        max = Math.max(max, target);

        return new int[]{ target, min, max };
    }

    private int[] calculateFatAndCarbs(UserProfileModel user, double tdee,
                                       List<String> conditions, int proteinTargetG) {
        String goal = upper(user.getGoal(), "MAINTENANCE");
        boolean hasIbs = conditions.contains("IBS");

        int remaining = Math.max((int) Math.round(tdee) - proteinTargetG * 4, 0);

        if (conditions.contains("DIABETES")) {
            int carbsCals = Math.min((int) Math.round(tdee * 0.40), remaining);
            int carbsTarget = carbsCals / 4;
            int fatTarget = Math.max((remaining - carbsTarget * 4) / 9, 0);
            return new int[]{ fatTarget, carbsTarget };
        }

        double fatPct = "WEIGHT_LOSS".equals(goal) ? 0.25 : 0.30;
        if (conditions.contains("HIGH_CHOLESTEROL")) fatPct = Math.min(fatPct, 0.25);
        if (hasIbs) fatPct = Math.min(fatPct, 0.25);

        int fatTarget = (int) Math.round(tdee * fatPct) / 9;
        int carbsTarget = Math.max((remaining - fatTarget * 9) / 4, 0);
        return new int[]{ fatTarget, carbsTarget };
    }

    private int[] adjustCarbsMinimum(int[] fatAndCarbs, List<String> conditions) {
        final int CARBS_MIN = 130;
        int fat = fatAndCarbs[0];
        int carbs = fatAndCarbs[1];

        if (!conditions.contains("DIABETES") && carbs < CARBS_MIN) {
            int steal = (CARBS_MIN - carbs) * 4;
            if (fat * 9 >= steal) {
                fat -= steal / 9;
                carbs = CARBS_MIN;
            }
        }
        return new int[]{ fat, carbs };
    }

    private MacroTarget buildMacroTarget(int[] protein, int[] fatCarbs,
                                         double tdee, List<String> conditions, int meals) {
        int fatTarget  = fatCarbs[0];
        int carbsTarget = fatCarbs[1];

        int fatMin = (int) Math.round((tdee * 0.20) / 9.0);
        int fatMax = (int) Math.round((tdee * 0.35) / 9.0);
        fatTarget = Math.max(fatTarget, fatMin);
        int saturatedFatMax = conditions.contains("HIGH_CHOLESTEROL")
                ? (int) Math.round((tdee * 0.07) / 9.0)
                : (int) Math.round((tdee * 0.10) / 9.0);

        boolean lowCarbWarning = carbsTarget < 130;

        return new MacroTarget(
                protein[0], protein[1], protein[2],
                fatTarget, fatMin, fatMax, saturatedFatMax,
                carbsTarget, 130, lowCarbWarning,
                meals, conditions.contains("IBS"));
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
