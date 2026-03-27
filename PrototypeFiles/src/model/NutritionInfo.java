package model;

import com.google.gson.annotations.SerializedName;

public class NutritionInfo {
    @SerializedName("serving_size")
    private String servingSize;

    @SerializedName("servings_per_recipe")
    private int servingsPerRecipe;

    private double calories;

    @SerializedName("calories_from_fat")
    private double caloriesFromFat;

    @SerializedName("total_fat_g")
    private double totalFatG;

    @SerializedName("saturated_fat_g")
    private double saturatedFatG;

    @SerializedName("cholesterol_mg")
    private double cholesterolMg;

    @SerializedName("sodium_mg")
    private double sodiumMg;

    @SerializedName("total_carbs_g")
    private double totalCarbsG;

    @SerializedName("dietary_fiber_g")
    private double dietaryFiberG;

    @SerializedName("sugars_g")
    private double sugarsG;

    @SerializedName("protein_g")
    private double proteinG;

    public String getServingSize() {
        return servingSize;
    }

    public int getServingsPerRecipe() {
        return servingsPerRecipe;
    }

    public double getCalories() {
        return calories;
    }

    public double getCaloriesFromFat() {
        return caloriesFromFat;
    }

    public double getTotalFatG() {
        return totalFatG;
    }

    public double getSaturatedFatG() {
        return saturatedFatG;
    }

    public double getCholesterolMg() {
        return cholesterolMg;
    }

    public double getSodiumMg() {
        return sodiumMg;
    }

    public double getTotalCarbsG() {
        return totalCarbsG;
    }

    public double getDietaryFiberG() {
        return dietaryFiberG;
    }

    public double getSugarsG() {
        return sugarsG;
    }

    public double getProteinG() {
        return proteinG;
    }
}
