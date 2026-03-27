package service;

import model.UserProfile;

import java.util.LinkedHashMap;
import java.util.Map;

public class CalorieCalculator {

    public static double calculateBMR(UserProfile user) {
        double bmr = 10 * user.getWeightKg() + 6.25 * user.getHeightCm() - 5 * user.getAge();
        if ("MALE".equalsIgnoreCase(user.getGender())) {
            bmr += 5;
        } else {
            bmr -= 161;
        }
        return bmr;
    }

    public static double calculateTDEE(UserProfile user) {
        double bmr = calculateBMR(user);
        double multiplier;
        switch (user.getActivityLevel().toUpperCase()) {
            case "SEDENTARY":
                multiplier = 1.2;
                break;
            case "LIGHT":
                multiplier = 1.375;
                break;
            case "MODERATE":
                multiplier = 1.55;
                break;
            case "ACTIVE":
                multiplier = 1.725;
                break;
            case "VERY_ACTIVE":
                multiplier = 1.9;
                break;
            default:
                multiplier = 1.2;
                break;
        }
        return bmr * multiplier;
    }

    public static double adjustForGoal(UserProfile user) {
        double tdee = calculateTDEE(user);
        int adjustment = getGoalAdjustment(user.getGoalIntensity());

        switch (user.getGoal().toUpperCase()) {
            case "WEIGHT_LOSS":
                return tdee - adjustment;
            case "WEIGHT_GAIN":
                return tdee + adjustment;
            case "MAINTENANCE":
            default:
                return tdee;
        }
    }

    private static int getGoalAdjustment(String intensity) {
        switch (intensity.toUpperCase()) {
            case "SLOW":
                return 250;
            case "NORMAL":
                return 500;
            case "FAST":
                return 750;
            default:
                return 500;
        }
    }

    public static double[] getDailyTargets(UserProfile user) {
        double baseTarget = adjustForGoal(user);
        double[] targets = new double[7];

        if (user.isZigzag()) {

            for (int i = 0; i < 7; i++) {
                if (i % 2 == 0) {
                    targets[i] = baseTarget * 1.10;
                } else {
                    targets[i] = baseTarget * 0.90;
                }
            }
        } else {
            for (int i = 0; i < 7; i++) {
                targets[i] = baseTarget;
            }
        }
        return targets;
    }

    public static Map<String, Double> getSlotRatios(int mealsPerDay) {
        Map<String, Double> slots = new LinkedHashMap<>();
        switch (mealsPerDay) {
            case 3:
                slots.put("breakfast", 0.30);
                slots.put("lunch", 0.40);
                slots.put("dinner", 0.30);
                break;
            case 4:
                slots.put("breakfast", 0.25);
                slots.put("lunch", 0.35);
                slots.put("dinner", 0.30);
                slots.put("snack", 0.10);
                break;
            case 5:
                slots.put("breakfast", 0.20);
                slots.put("lunch", 0.30);
                slots.put("dinner", 0.30);
                slots.put("snack", 0.10);
                slots.put("snack_2", 0.10);
                break;
            default:

                slots.put("breakfast", 0.30);
                slots.put("lunch", 0.40);
                slots.put("dinner", 0.30);
                break;
        }

        double ratioSum = 0;
        for (double r : slots.values())
            ratioSum += r;
        if (Math.abs(ratioSum - 1.0) > 0.01) {
            System.err.println("[CalorieCalculator] WARNING: slot ratios sum to "
                    + ratioSum + " for mealsPerDay=" + mealsPerDay
                    + " (expected 1.0). ILP calorie bounds may be incorrect!");
        }
        return slots;
    }

    public static double getWeeklyTotal(double[] dailyTargets) {
        double sum = 0;
        for (double d : dailyTargets)
            sum += d;
        return sum;
    }
}