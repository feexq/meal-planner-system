package model.output;

import java.util.List;

public class RecipeCandidate {
    private int recipeId;
    private String recipeName;
    private int score;
    private double caloriesPerServing;
    private double recommendedServings;
    private double scaledCalories;
    private double caloricDeviation;
    private List<String> matchedHealthTags;
    private List<String> appliedNutritionThresholds;
    private List<String> dietaryNotes;
    private List<String> relaxations;
    private ScoreBreakdown scoreBreakdown;
    private List<String> ingredients;

    public RecipeCandidate(int recipeId, String recipeName, int score,
            double caloriesPerServing, double recommendedServings,
            double scaledCalories, double caloricDeviation,
            List<String> matchedHealthTags,
            List<String> appliedNutritionThresholds,
            List<String> dietaryNotes,
            List<String> relaxations,
            ScoreBreakdown scoreBreakdown,
            List<String> ingredients) {
        this.recipeId = recipeId;
        this.recipeName = recipeName;
        this.score = score;
        this.caloriesPerServing = caloriesPerServing;
        this.recommendedServings = recommendedServings;
        this.scaledCalories = scaledCalories;
        this.caloricDeviation = caloricDeviation;
        this.matchedHealthTags = matchedHealthTags;
        this.appliedNutritionThresholds = appliedNutritionThresholds;
        this.dietaryNotes = dietaryNotes;
        this.relaxations = relaxations;
        this.scoreBreakdown = scoreBreakdown;
        this.ingredients = ingredients;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public int getScore() {
        return score;
    }

    public double getCaloriesPerServing() {
        return caloriesPerServing;
    }

    public double getRecommendedServings() {
        return recommendedServings;
    }

    public double getScaledCalories() {
        return scaledCalories;
    }

    public double getCaloricDeviation() {
        return caloricDeviation;
    }

    public List<String> getMatchedHealthTags() {
        return matchedHealthTags;
    }

    public List<String> getAppliedNutritionThresholds() {
        return appliedNutritionThresholds;
    }

    public List<String> getDietaryNotes() {
        return dietaryNotes;
    }

    public List<String> getRelaxations() {
        return relaxations;
    }

    public ScoreBreakdown getScoreBreakdown() {
        return scoreBreakdown;
    }

    public List<String> getIngredients() {
        return ingredients;
    }
}
