package com.feex.mealplannersystem.mealplan.service.filter;

import com.feex.mealplannersystem.mealplan.dto.FilterResult;
import com.feex.mealplannersystem.mealplan.mapper.context.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.context.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
public class RecipeFilterService {

    private static final double DIABETES_MAX_SUGARS_G   = 10.0;
    private static final double DIABETES_MAX_CARBS_G    = 45.0;
    private static final double HYPERTENSION_MAX_SODIUM = 600.0;
    private static final double HIGH_CHOLESTEROL_MAX_FAT_G = 7.0;
    private static final double HIGH_CHOLESTEROL_MAX_CHOL_MG = 100.0;
    private static final double KIDNEY_DISEASE_MAX_SODIUM_MG = 400.0;
    private static final double KIDNEY_DISEASE_MAX_PROTEIN_G = 20.0;
    private static final double GOUT_MAX_PROTEIN_G = 25.0;

    private static final ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    public FilterResult filterRecipes(RecipeDataContext data, ClassificationContext classification,
                                      UserProfileModel user, String slotMealType) {

        Map<String, Integer> counts = initCounts();

        String userDiet = user.getDietType().toLowerCase();
        List<String> userConditions = lower(user.getHealthConditions());
        List<String> userAllergies  = lower(user.getAllergies());
        List<String> userDisliked   = new ArrayList<>(lower(user.getDislikedIngredients()));

        if (userConditions.contains("gastritis")) {
            addIfAbsent(userDisliked, "fried_food");
            addIfAbsent(userDisliked, "spicy");
        }
        if (userConditions.contains("gerd")) {
            addIfAbsent(userDisliked, "spicy");
        }

        String effectiveMealType = "snack_2".equals(slotMealType) ? "snack" : slotMealType;

        List<RecipeModel> result = new ArrayList<>();

        for (RecipeModel recipe : data.getRecipes()) {

            if (!"all".equalsIgnoreCase(effectiveMealType)) {
                if (!matchesMealType(recipe.getMealType(), effectiveMealType)) {
                    counts.merge("meal_type_mismatch", 1, Integer::sum); continue;
                }
            }

            if ("1+ DAYS".equalsIgnoreCase(recipe.getCookTime())) {
                counts.merge("cook_time_exclude", 1, Integer::sum); continue;
            }

            NutritionModel nutrition = data.getNutrition(recipe.getId());
            if (nutrition == null) {
                counts.merge("missing_nutrition", 1, Integer::sum); continue;
            }

            if (!"omnivore".equals(userDiet) &&
                    classification.isNotSuitableFor(recipe.getParsedIngredients(), userDiet)) {
                counts.merge("diet_violation_ingredient", 1, Integer::sum); continue;
            }

            if (!"omnivore".equals(userDiet) &&
                    hasClassifiedViolation(recipe, Collections.singletonList(userDiet), classification)) {
                counts.merge("diet_violation_classified", 1, Integer::sum); continue;
            }

            if (hasClassifiedViolation(recipe, userConditions, classification)) {
                counts.merge("health_condition_classified", 1, Integer::sum); continue;
            }

            if (hasNumericViolation(nutrition, userConditions)) {
                counts.merge("health_condition_numeric", 1, Integer::sum); continue;
            }

            if (("dinner".equals(effectiveMealType) || "lunch".equals(effectiveMealType))
                    && recipe.getParsedIngredients().size() <= 5
                    && nutrition.getProteinG() < 5.0) {
                counts.merge("side_dish_excluded", 1, Integer::sum); continue;
            }

            if (hasAllergyViolation(recipe, userAllergies)) {
                counts.merge("allergy", 1, Integer::sum); continue;
            }

            if (hasDislikedIngredient(recipe, userDisliked)) {
                counts.merge("disliked_ingredient", 1, Integer::sum); continue;
            }

            result.add(recipe);
        }

        String note = null;
        if (result.isEmpty()) {
            note = "No recipes passed filtering. Top cause: " + counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .orElse("unknown");
        }
        return new FilterResult(result, note, counts);
    }

    public static List<String> getAppliedThresholds(List<String> userConditions) {
        List<String> t = new ArrayList<>();
        for (String c : userConditions) {
            switch (c.toLowerCase()) {
                case "diabetes"         -> { t.add("sugars_g ≤ " + DIABETES_MAX_SUGARS_G); t.add("total_carbs_g ≤ " + DIABETES_MAX_CARBS_G); }
                case "hypertension"     -> t.add("sodium_mg ≤ " + HYPERTENSION_MAX_SODIUM);
                case "high_cholesterol" -> { t.add("saturated_fat_g ≤ " + HIGH_CHOLESTEROL_MAX_FAT_G); t.add("cholesterol_mg ≤ " + HIGH_CHOLESTEROL_MAX_CHOL_MG); }
                case "kidney_disease"   -> { t.add("sodium_mg ≤ " + KIDNEY_DISEASE_MAX_SODIUM_MG); t.add("protein_g ≤ " + KIDNEY_DISEASE_MAX_PROTEIN_G); }
                case "gout"             -> t.add("protein_g ≤ " + GOUT_MAX_PROTEIN_G);
            }
        }
        return t;
    }


    private static boolean matchesMealType(String recipeMealType, String slotMealType) {
        if (recipeMealType == null)
            return false;
        String rt = recipeMealType.toLowerCase();
        String st = slotMealType.toLowerCase();
        if (rt.equals(st))
            return true;
        if ("snack".equals(st) && ("dessert".equals(rt) || "appetizers".equals(rt)))
            return true;
        return false;
    }

    public static boolean matchesAdjacentMealType(String recipeMealType, String slotMealType) {
        if (recipeMealType == null)
            return false;
        String rt = recipeMealType.toLowerCase();
        String st = slotMealType.toLowerCase();
        if (matchesMealType(recipeMealType, slotMealType))
            return true;
        switch (st) {
            case "breakfast":
                return "snack".equals(rt);
            case "lunch":
                return "dinner".equals(rt);
            case "dinner":
                return "lunch".equals(rt);
            case "snack":
                return "dessert".equals(rt) || "breakfast".equals(rt);
            default:
                return false;
        }
    }

    public static int complexityLevel(String c) {
        if (c == null) return 0;
        return switch (c.toLowerCase()) { case "easy" -> 1; case "medium" -> 2; case "hard" -> 3; default -> 0; };
    }

    public static int budgetLevel(String b) {
        if (b == null) return 0;
        return switch (b.toLowerCase()) { case "low" -> 1; case "medium" -> 2; case "high" -> 3; default -> 0; };
    }

    public static boolean exceedsComplexity(String recipeC, String userC) {
        if ("ANY".equalsIgnoreCase(userC)) return false;
        return complexityLevel(recipeC) > complexityLevel(userC);
    }

    public static boolean exceedsComplexityByMoreThanOne(String recipeC, String userC) {
        if ("ANY".equalsIgnoreCase(userC)) return false;
        return complexityLevel(recipeC) - complexityLevel(userC) > 1;
    }

    public static boolean exceedsBudget(String recipeB, String userB) {
        return budgetLevel(recipeB) > budgetLevel(userB);
    }

    public static boolean exceedsBudgetByMoreThanOne(String recipeB, String userB) {
        return budgetLevel(recipeB) - budgetLevel(userB) > 1;
    }

    private boolean hasClassifiedViolation(RecipeModel recipe, List<String> conditions,
                                            ClassificationContext ctx) {
        for (String cond : conditions) {
            if (ctx.hasForbiddenIngredient(recipe.getParsedIngredients(), cond)) return true;
        }
        return false;
    }

    private boolean hasNumericViolation(NutritionModel n, List<String> conditions) {
        for (String c : conditions) {
            boolean bad = switch (c) {
                case "diabetes"         -> n.getSugarsG() > DIABETES_MAX_SUGARS_G || n.getTotalCarbsG() > DIABETES_MAX_CARBS_G;
                case "hypertension"     -> n.getSodiumMg() > HYPERTENSION_MAX_SODIUM;
                case "high_cholesterol" -> n.getSaturatedFatG() > HIGH_CHOLESTEROL_MAX_FAT_G || n.getCholesterolMg() > HIGH_CHOLESTEROL_MAX_CHOL_MG;
                case "kidney_disease"   -> n.getSodiumMg() > KIDNEY_DISEASE_MAX_SODIUM_MG || n.getProteinG() > KIDNEY_DISEASE_MAX_PROTEIN_G;
                case "gout"             -> n.getProteinG() > GOUT_MAX_PROTEIN_G;
                default                 -> false;
            };
            if (bad) return true;
        }
        return false;
    }

    private boolean hasAllergyViolation(RecipeModel recipe, List<String> allergies) {
        for (String allergy : allergies) {
            for (String ing : recipe.getParsedIngredients()) {
                if (ing.contains(allergy)) return true;
            }
        }
        return false;
    }

    public static boolean hasDislikedIngredient(RecipeModel recipe, List<String> disliked) {
        if (disliked.isEmpty())
            return false;
        String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
        for (String d : disliked) {
            String dNorm = d.replace("_", " ").toLowerCase();
            String searchKeyword = dNorm.equals("fried food") ? "fried" : dNorm;

            String searchKeywordSingular = searchKeyword.endsWith("s") && searchKeyword.length() > 3
                    ? searchKeyword.substring(0, searchKeyword.length() - 1)
                    : searchKeyword;

            if (containsWordBoundary(recipeName, searchKeyword))
                return true;

            for (String ingredientName : recipe.getParsedIngredients()) {
                if (containsWordBoundary(ingredientName.toLowerCase(), searchKeywordSingular))
                    return true;
            }
        }
        return false;
    }

    static boolean containsWordBoundary(String text, String keyword) {
        if (text == null || keyword == null)
            return false;
        Pattern p = PATTERN_CACHE.computeIfAbsent(
                keyword, k -> Pattern.compile("\\b" + Pattern.quote(k) + "\\b"));
        return p.matcher(text).find();
    }

    private static List<String> lower(List<String> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(String::toLowerCase).collect(Collectors.toList());
    }

    private static void addIfAbsent(List<String> list, String value) {
        if (!list.contains(value)) list.add(value);
    }

    private static Map<String, Integer> initCounts() {
        Map<String, Integer> m = new LinkedHashMap<>();
        List.of("meal_type_mismatch","semantic_ban","celiac_gluten_ingredient","missing_nutrition",
                "diet_violation_ingredient","diet_violation_classified","health_condition_classified",
                "health_condition_numeric","allergy","disliked_ingredient",
                "cook_time_exclude","side_dish_excluded").forEach(k -> m.put(k, 0));
        return m;
    }
}
