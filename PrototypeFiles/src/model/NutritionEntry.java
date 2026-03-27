package model;

public class NutritionEntry {
    private int id;
    private String name;
    private NutritionInfo nutrition;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NutritionInfo getNutrition() {
        return nutrition;
    }
}
