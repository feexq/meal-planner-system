package model;

import java.util.List;

public class UserProfile {
    private String userId;
    private String gender;
    private int age;
    private int heightCm;
    private double weightKg;
    private String activityLevel;
    private String goal;
    private String goalIntensity;
    private String dietType;
    private List<String> healthConditions;
    private List<String> allergies;
    private List<String> dislikedIngredients;
    private int mealsPerDay;
    private String cookingComplexity;
    private String budgetLevel;
    private boolean zigzag;
    private Integer maxRecipeRepeatsPerWeek = 2;

    public String getUserId() {
        return userId;
    }

    public String getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }

    public int getHeightCm() {
        return heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public String getGoal() {
        return goal;
    }

    public String getGoalIntensity() {
        return goalIntensity;
    }

    public String getDietType() {
        return dietType;
    }

    public List<String> getHealthConditions() {
        return healthConditions;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public List<String> getDislikedIngredients() {
        return dislikedIngredients;
    }

    public int getMealsPerDay() {
        return mealsPerDay;
    }

    public String getCookingComplexity() {
        return cookingComplexity;
    }

    public String getBudgetLevel() {
        return budgetLevel;
    }

    public boolean isZigzag() {
        return zigzag;
    }

    public Integer getMaxRecipeRepeatsPerWeek() {
        return maxRecipeRepeatsPerWeek;
    }
}
