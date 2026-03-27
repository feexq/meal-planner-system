package model.output;

import java.util.List;

public class DayPlan {
    private int day;
    private int dailyCalorieTarget;
    private EstimatedDailyMacros estimatedDailyMacros;
    private List<MealSlot> slots;
    private double relaxationRate;

    public DayPlan(int day, int dailyCalorieTarget,
            EstimatedDailyMacros estimatedDailyMacros, List<MealSlot> slots, double relaxationRate) {
        this.day = day;
        this.dailyCalorieTarget = dailyCalorieTarget;
        this.estimatedDailyMacros = estimatedDailyMacros;
        this.slots = slots;
        this.relaxationRate = relaxationRate;
    }

    public int getDay() {
        return day;
    }

    public int getDailyCalorieTarget() {
        return dailyCalorieTarget;
    }

    public EstimatedDailyMacros getEstimatedDailyMacros() {
        return estimatedDailyMacros;
    }

    public List<MealSlot> getSlots() {
        return slots;
    }

    public double getRelaxationRate() {
        return relaxationRate;
    }
}
