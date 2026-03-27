package model.output;

import java.util.List;

public class WeeklyMealPlan {
    private String userId;
    private int dailyCalorieTarget;
    private int weeklyCalorieTarget;
    private double relaxationRate;
    private boolean lowCoverageWarning;
    private List<DayPlan> days;

    public WeeklyMealPlan(String userId, int dailyCalorieTarget,
            int weeklyCalorieTarget, List<DayPlan> days, double relaxationRate, boolean lowCoverageWarning) {
        this.userId = userId;
        this.dailyCalorieTarget = dailyCalorieTarget;
        this.weeklyCalorieTarget = weeklyCalorieTarget;
        this.days = days;
        this.relaxationRate = relaxationRate;
        this.lowCoverageWarning = lowCoverageWarning;
    }

    public String getUserId() {
        return userId;
    }

    public int getDailyCalorieTarget() {
        return dailyCalorieTarget;
    }

    public int getWeeklyCalorieTarget() {
        return weeklyCalorieTarget;
    }

    public List<DayPlan> getDays() {
        return days;
    }

    public double getRelaxationRate() {
        return relaxationRate;
    }

    public boolean isLowCoverageWarning() {
        return lowCoverageWarning;
    }
}
