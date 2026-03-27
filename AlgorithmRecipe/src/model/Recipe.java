package model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Recipe {
    private int id;
    private String name;
    private String description;
    private String ingredients;

    @SerializedName("ingredients_raw_str")
    private String ingredientsRawStr;

    @SerializedName("serving_size")
    private String servingSize;

    private int servings;
    private String steps;
    private List<String> tags;

    @SerializedName("meal_type")
    private String mealType;

    @SerializedName("cook_time")
    private String cookTime;

    @SerializedName("cook_complexity")
    private String cookComplexity;

    @SerializedName("cook_budget")
    private String cookBudget;

    private transient List<String> parsedIngredients;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getIngredientsRawStr() {
        return ingredientsRawStr;
    }

    public String getServingSize() {
        return servingSize;
    }

    public int getServings() {
        return servings;
    }

    public String getSteps() {
        return steps;
    }

    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }

    public String getMealType() {
        return mealType;
    }

    public String getCookTime() {
        return cookTime;
    }

    public String getCookComplexity() {
        return cookComplexity;
    }

    public String getCookBudget() {
        return cookBudget;
    }

    public List<String> getParsedIngredients() {
        return parsedIngredients != null ? parsedIngredients : new ArrayList<>();
    }

    public void setParsedIngredients(List<String> parsedIngredients) {
        this.parsedIngredients = parsedIngredients;
    }

    public void parseIngredients() {
        this.parsedIngredients = new ArrayList<>();
        if (ingredients == null || ingredients.isEmpty())
            return;

        String s = ingredients.trim();

        if (s.startsWith("["))
            s = s.substring(1);
        if (s.endsWith("]"))
            s = s.substring(0, s.length() - 1);

        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = '\'';

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!inQuote && (c == '\'' || c == '"')) {
                inQuote = true;
                quoteChar = c;
            } else if (inQuote && c == quoteChar) {
                inQuote = false;
            } else if (!inQuote && c == ',') {
                String item = current.toString().trim().toLowerCase();
                if (!item.isEmpty()) {
                    parsedIngredients.add(item);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        String last = current.toString().trim().toLowerCase();
        if (!last.isEmpty()) {
            parsedIngredients.add(last);
        }
    }
}
