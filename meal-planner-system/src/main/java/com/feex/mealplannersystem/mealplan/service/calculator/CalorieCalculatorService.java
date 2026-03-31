package com.feex.mealplannersystem.mealplan.service.calculator;

import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class CalorieCalculatorService {

    public double calculateBMR(UserProfileModel user) {
        double bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * user.getAge();
        return "MALE".equalsIgnoreCase(user.getGender()) ? bmr + 5 : bmr - 161;
    }

    public double calculateTDEE(UserProfileModel user) {
        double bmr = calculateBMR(user);
        double multiplier = switch (user.getActivityLevel().toUpperCase()) {
            case "SEDENTARY"   -> 1.2;
            case "LIGHT"       -> 1.375;
            case "MODERATE"    -> 1.55;
            case "ACTIVE"      -> 1.725;
            case "VERY_ACTIVE" -> 1.9;
            default            -> 1.2;
        };
        return bmr * multiplier;
    }

    public double adjustForGoal(UserProfileModel user) {
        double tdee = calculateTDEE(user);
        int adjustment = goalAdjustment(user.getGoalIntensity());
        return switch (user.getGoal().toUpperCase()) {
            case "WEIGHT_LOSS" -> tdee - adjustment;
            case "WEIGHT_GAIN" -> tdee + adjustment;
            default            -> tdee;
        };
    }

    public double[] getDailyTargets(UserProfileModel user) {
        double base = adjustForGoal(user);
        double[] targets = new double[7];
        if (user.isZigzag()) {
            for (int i = 0; i < 7; i++) targets[i] = base * (i % 2 == 0 ? 1.10 : 0.90);
        } else {
            Arrays.fill(targets, base);
        }
        return targets;
    }

    public double getWeeklyTotal(double[] dailyTargets) {
        double sum = 0;
        for (double d : dailyTargets) sum += d;
        return sum;
    }

    public Map<String, Double> getSlotRatios(int mealsPerDay) {
        Map<String, Double> slots = new LinkedHashMap<>();
        switch (mealsPerDay) {
            case 3 -> {
                slots.put("breakfast", 0.30);
                slots.put("lunch",     0.40);
                slots.put("dinner",    0.30);
            }
            case 4 -> {
                slots.put("breakfast", 0.25);
                slots.put("lunch",     0.35);
                slots.put("dinner",    0.30);
                slots.put("snack",     0.10);
            }
            case 5 -> {
                slots.put("breakfast", 0.20);
                slots.put("lunch",     0.30);
                slots.put("dinner",    0.30);
                slots.put("snack",     0.10);
                slots.put("snack_2",   0.10);
            }
            default -> {
                slots.put("breakfast", 0.30);
                slots.put("lunch",     0.40);
                slots.put("dinner",    0.30);
            }
        }
        return slots;
    }

    private static int goalAdjustment(String intensity) {
        if (intensity == null) return 500;
        return switch (intensity.toUpperCase()) {
            case "SLOW"   -> 250;
            case "NORMAL" -> 500;
            case "FAST"   -> 750;
            default       -> 500;
        };
    }
}
