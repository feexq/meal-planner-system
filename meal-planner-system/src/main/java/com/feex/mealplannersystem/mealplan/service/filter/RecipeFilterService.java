package com.feex.mealplannersystem.mealplan.service.filter;

import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
public class RecipeFilterService {

    private static final java.util.concurrent.ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

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

    public enum ConditionSeverity { CRITICAL, HIGH, MODERATE, MILD }

    public static ConditionSeverity getConditionSeverity(String condition) {
        return switch (condition.toLowerCase()) {
            case "celiac_disease", "nut_allergy", "shellfish_allergy", "fish_allergy" -> ConditionSeverity.CRITICAL;
            case "diabetes", "kidney_disease"                                          -> ConditionSeverity.HIGH;
            case "hypertension", "high_cholesterol", "gout", "pancreatitis"           -> ConditionSeverity.MODERATE;
            default                                                                    -> ConditionSeverity.MILD;
        };
    }

    public static List<String> getAppliedThresholds(List<String> userConditions) {
        List<String> t = new ArrayList<>();
        for (String c : userConditions) {
            switch (c.toLowerCase()) {
                case "diabetes"         -> { t.add("sugars_g ≤ 10.0"); t.add("total_carbs_g ≤ 45.0"); }
                case "hypertension"     -> t.add("sodium_mg ≤ 600.0");
                case "high_cholesterol" -> { t.add("saturated_fat_g ≤ 7.0"); t.add("cholesterol_mg ≤ 100.0"); }
                case "kidney_disease"   -> { t.add("sodium_mg ≤ 400.0"); t.add("protein_g ≤ 20.0"); }
                case "gout"             -> t.add("protein_g ≤ 25.0");
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

    private static final Set<String> GLUTEN_SAFE = Set.of(
            "corn tortilla","corn flour","rice flour","rice bread","gluten-free bread",
            "gluten-free flour","gluten-free pasta","gluten-free noodle","gluten-free cracker",
            "gluten-free soy sauce","tamari","rice noodle","rice paper","cornbread",
            "corn starch","arrowroot","white miso","yellow miso","red miso","rice miso",
            "shiro miso","sweet miso");

    private static final List<String> GLUTEN_KW = List.of(
            "wheat","flour","barley","rye","spelt","kamut","triticale",
            "bread","toast","croissant","baguette","pita","pretzel",
            "pasta","spaghetti","macaroni","noodle","couscous","orzo",
            "fettuccine","linguine","penne","lasagna","ravioli","gnocchi",
            "semolina","bulgur","pastry","crouton","cracker","breadcrumb",
            "panko","tortilla","filo","phyllo","dumpling","wonton",
            "soy sauce","teriyaki","mugi miso","barley miso",
            "seitan","wheat germ","wheat bran","malt","malt vinegar");

    public static boolean containsGlutenIngredient(RecipeModel recipe) {
        for (String ing : recipe.getParsedIngredients()) {
            String il = ing.toLowerCase().trim();
            if (GLUTEN_SAFE.stream().anyMatch(il::contains)) continue;
            if (GLUTEN_KW.stream().anyMatch(il::contains)) return true;
        }
        return false;
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
                case "diabetes"         -> n.getSugarsG() > 10.0 || n.getTotalCarbsG() > 45.0;
                case "hypertension"     -> n.getSodiumMg() > 600.0;
                case "high_cholesterol" -> n.getSaturatedFatG() > 7.0 || n.getCholesterolMg() > 100.0;
                case "kidney_disease"   -> n.getSodiumMg() > 400.0 || n.getProteinG() > 20.0;
                case "gout"             -> n.getProteinG() > 25.0;
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

    public record FilterResult(List<RecipeModel> recipes, String filteringNote,
                                Map<String, Integer> eliminationCounts) {}
}
