package com.feex.mealplannersystem.domain.nutrition; // Перевірте свій пакет!

import com.feex.mealplannersystem.common.MealType;
import com.feex.mealplannersystem.repository.entity.preference.UserAllergyEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserDislikedIngredientEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserHealthConditionEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecipeFilter {

    private final ClassifiedIngredientsService classifiedIngredientsService;

    public RecipeFilter(ClassifiedIngredientsService classifiedIngredientsService) {
        this.classifiedIngredientsService = classifiedIngredientsService;
    }

    public enum ConditionSeverity {
        CRITICAL, HIGH, MODERATE, MILD
    }

    public ConditionSeverity getConditionSeverity(String condition) {
        switch (condition.toLowerCase()) {
            case "celiac_disease":
            case "nut_allergy":
            case "shellfish_allergy":
            case "fish_allergy":
                return ConditionSeverity.CRITICAL;
            case "diabetes":
            case "kidney_disease":
                return ConditionSeverity.HIGH;
            case "hypertension":
            case "high_cholesterol":
            case "gout":
            case "pancreatitis":
                return ConditionSeverity.MODERATE;
            case "ibs":
            case "gastritis":
            case "gerd":
            case "lactose_intolerance":
            default:
                return ConditionSeverity.MILD;
        }
    }

    public FilterResult filterRecipes(List<RecipeEntity> allRecipes, UserPreferenceEntity user, String slotMealType) {
        Map<String, Integer> eliminationCounts = new LinkedHashMap<>();
        eliminationCounts.put("meal_type_mismatch", 0);
        eliminationCounts.put("missing_nutrition", 0);
        eliminationCounts.put("diet_violation_classified", 0);
        eliminationCounts.put("health_condition_classified", 0);
        eliminationCounts.put("health_condition_numeric", 0);
        eliminationCounts.put("allergy", 0);
        eliminationCounts.put("disliked_ingredient", 0);
        eliminationCounts.put("cook_time_exclude", 0);
        eliminationCounts.put("side_dish_excluded", 0);

        String userDiet = user.getDietType() != null ? user.getDietType().name().toLowerCase() : "omnivore";

        List<String> userConditions = user.getHealthConditions().stream()
                .map(UserHealthConditionEntity::getConditionName)
                .map(String::toLowerCase).collect(Collectors.toList());

        List<String> userAllergies = user.getAllergies().stream()
                .map(UserAllergyEntity::getAllergyName)
                .map(String::toLowerCase).collect(Collectors.toList());

        List<String> userDisliked = user.getDislikedIngredients().stream()
                .map(UserDislikedIngredientEntity::getIngredientName)
                .map(String::toLowerCase).collect(Collectors.toList());

        if (userConditions.contains("gastritis")) {
            if (!userDisliked.contains("fried_food")) userDisliked.add("fried_food");
            if (!userDisliked.contains("spicy")) userDisliked.add("spicy");
        }

        if (userConditions.contains("gerd")) {
            if (!userDisliked.contains("spicy")) userDisliked.add("spicy");
        }

        String effectiveMealType = slotMealType.equals("snack_2") ? "snack" : slotMealType;
        List<RecipeEntity> result = new ArrayList<>();

        for (RecipeEntity recipe : allRecipes) {

            if (!"all".equalsIgnoreCase(effectiveMealType)) {
                if (!matchesMealType(recipe.getMealType(), effectiveMealType)) {
                    eliminationCounts.merge("meal_type_mismatch", 1, Integer::sum);
                    continue;
                }
            }

            if (recipe.getCookTime() != null && "1+ days".equalsIgnoreCase(recipe.getCookTime().name())) {
                eliminationCounts.merge("cook_time_exclude", 1, Integer::sum);
                continue;
            }

            RecipeNutritionEntity nutrition = recipe.getNutrition();
            if (nutrition == null) {
                eliminationCounts.merge("missing_nutrition", 1, Integer::sum);
                continue;
            }

            if (!"omnivore".equals(userDiet) && classifiedIngredientsService.hasForbiddenIngredient(recipe.getIngredients(), userDiet)) {
                eliminationCounts.merge("diet_violation_classified", 1, Integer::sum);
                continue;
            }

            boolean condViol = false;
            for (String condition : userConditions) {
                if (classifiedIngredientsService.hasForbiddenIngredient(recipe.getIngredients(), condition)) {
                    condViol = true;
                    break;
                }
            }
            if (condViol) {
                eliminationCounts.merge("health_condition_classified", 1, Integer::sum);
                continue;
            }

            if (hasHealthConditionNumericViolation(nutrition, userConditions)) {
                eliminationCounts.merge("health_condition_numeric", 1, Integer::sum);
                continue;
            }

            if (("dinner".equals(effectiveMealType) || "lunch".equals(effectiveMealType))
                    && recipe.getIngredients().size() <= 5
                    && nutrition.getProteinG().doubleValue() < 5.0) {
                eliminationCounts.merge("side_dish_excluded", 1, Integer::sum);
                continue;
            }

            if (hasAllergyViolation(recipe, userAllergies)) {
                eliminationCounts.merge("allergy", 1, Integer::sum);
                continue;
            }

            if (hasDislikedIngredient(recipe, userDisliked)) {
                eliminationCounts.merge("disliked_ingredient", 1, Integer::sum);
                continue;
            }

            result.add(recipe);
        }

        String filteringNote = null;
        if (result.isEmpty()) {
            String topFilter = eliminationCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + e.getValue() + " recipes)")
                    .orElse("unknown");
            filteringNote = "No recipes passed filtering. Top elimination cause: " + topFilter;
        }

        return new FilterResult(result, filteringNote, eliminationCounts);
    }

    public List<String> getAppliedThresholds(List<String> userConditions) {
        List<String> thresholds = new ArrayList<>();
        for (String condition : userConditions) {
            switch (condition.toLowerCase()) {
                case "diabetes":
                    thresholds.add("sugars_g \u2264 10.0");
                    thresholds.add("total_carbs_g \u2264 45.0");
                    break;
                case "hypertension":
                    thresholds.add("sodium_mg \u2264 600.0");
                    break;
                case "high_cholesterol":
                    thresholds.add("saturated_fat_g \u2264 7.0");
                    thresholds.add("cholesterol_mg \u2264 100.0");
                    break;
                case "kidney_disease":
                    thresholds.add("sodium_mg \u2264 400.0");
                    thresholds.add("protein_g \u2264 20.0");
                    break;
                case "gout":
                    thresholds.add("protein_g \u2264 25.0");
                    break;
            }
        }
        return thresholds;
    }

    private boolean hasHealthConditionNumericViolation(RecipeNutritionEntity n, List<String> conditions) {
        if (conditions.isEmpty() || n == null) return false;

        for (String condition : conditions) {
            switch (condition.toLowerCase()) {
                case "diabetes":
                    if (n.getSugarsG() != null && n.getSugarsG().doubleValue() > 10.0) return true;
                    if (n.getTotalCarbsG() != null && n.getTotalCarbsG().doubleValue() > 45.0) return true;
                    break;
                case "hypertension":
                    if (n.getSodiumMg() != null && n.getSodiumMg().doubleValue() > 600.0) return true;
                    break;
                case "high_cholesterol":
                    if (n.getSaturatedFatG() != null && n.getSaturatedFatG().doubleValue() > 7.0) return true;
                    if (n.getCholesterolMg() != null && n.getCholesterolMg().doubleValue() > 100.0) return true;
                    break;
                case "kidney_disease":
                    if (n.getSodiumMg() != null && n.getSodiumMg().doubleValue() > 400.0) return true;
                    if (n.getProteinG() != null && n.getProteinG().doubleValue() > 20.0) return true;
                    break;
                case "gout":
                    if (n.getProteinG() != null && n.getProteinG().doubleValue() > 25.0) return true;
                    break;
            }
        }
        return false;
    }

    public boolean hasAllergyViolation(RecipeEntity recipe, List<String> allergies) {
        if (allergies.isEmpty()) return false;
        String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";

        for (String allergy : allergies) {
            String allergyNorm = allergy.toLowerCase().replace("_", " ").trim();
            List<String> allergyKeywords = getAllergyKeywords(allergyNorm);

            for (String keyword : allergyKeywords) {
                if (containsWordBoundary(recipeName, keyword)) return true;

                for (RecipeIngredientEntity ing : recipe.getIngredients()) {
                    if (containsWordBoundary(ing.getRawName().toLowerCase(), keyword)) return true;
                }
            }
        }
        return false;
    }

    public boolean hasDislikedIngredient(RecipeEntity recipe, List<String> disliked) {
        if (disliked.isEmpty()) return false;
        String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";

        for (String d : disliked) {
            String dNorm = d.replace("_", " ").toLowerCase();
            String searchKeyword = dNorm.equals("fried food") ? "fried" : dNorm;
            String searchKeywordSingular = searchKeyword.endsWith("s") && searchKeyword.length() > 3
                    ? searchKeyword.substring(0, searchKeyword.length() - 1) : searchKeyword;

            if (containsWordBoundary(recipeName, searchKeyword)) return true;

            for (RecipeIngredientEntity ing : recipe.getIngredients()) {
                if (containsWordBoundary(ing.getRawName().toLowerCase(), searchKeywordSingular)) return true;
            }
        }
        return false;
    }

    private boolean matchesMealType(MealType recipeMealType, String slotMealType) {
        if (recipeMealType == null) return false;
        String rt = recipeMealType.name().toLowerCase();
        String st = slotMealType.toLowerCase();
        if (rt.equals(st)) return true;
        if ("snack".equals(st) && ("dessert".equals(rt) || "appetizers".equals(rt))) return true;
        return false;
    }

    public boolean matchesAdjacentMealType(MealType recipeMealType, String slotMealType) {
        if (recipeMealType == null) return false;
        String rt = recipeMealType.name().toLowerCase();
        String st = slotMealType.toLowerCase();
        if (matchesMealType(recipeMealType, slotMealType)) return true;

        switch (st) {
            case "breakfast": return "snack".equals(rt);
            case "lunch": return "dinner".equals(rt);
            case "dinner": return "lunch".equals(rt);
            case "snack": return "dessert".equals(rt) || "breakfast".equals(rt);
            default: return false;
        }
    }

    private boolean containsWordBoundary(String text, String keyword) {
        if (text == null || keyword == null) return false;
        String regex = "\\b" + Pattern.quote(keyword) + "\\b";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private String toSingularSafe(String word) {
        if (word == null || word.length() <= 3) return word;
        if (word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y";
        if (word.endsWith("oes")) return word.substring(0, word.length() - 2);
        if (word.endsWith("s") && !word.endsWith("ss")) return word.substring(0, word.length() - 1);
        return word;
    }

    private List<String> getAllergyKeywords(String allergyNorm) {
        List<String> keywords = new ArrayList<>();
        keywords.add(allergyNorm);
        String singular = toSingularSafe(allergyNorm);
        if (!singular.equals(allergyNorm)) keywords.add(singular);
        if (allergyNorm.contains("allergy")) {
            String base = allergyNorm.replace("allergy", "").trim();
            if (!base.isEmpty()) {
                keywords.add(base);
                String baseSingular = toSingularSafe(base);
                if (!baseSingular.equals(base)) keywords.add(baseSingular);
            }
        }
        return keywords;
    }

    public static class FilterResult {
        private final List<RecipeEntity> recipes;
        private final String filteringNote;
        private final Map<String, Integer> eliminationCounts;

        public FilterResult(List<RecipeEntity> recipes, String filteringNote, Map<String, Integer> eliminationCounts) {
            this.recipes = recipes;
            this.filteringNote = filteringNote;
            this.eliminationCounts = eliminationCounts;
        }

        public List<RecipeEntity> getRecipes() { return recipes; }
        public String getFilteringNote() { return filteringNote; }
        public Map<String, Integer> getEliminationCounts() { return eliminationCounts; }
    }
}