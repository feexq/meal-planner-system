package com.feex.mealplannersystem.mealplan.service.scorer;

import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.calculator.MacroRequirementService.MacroTarget;
import com.feex.mealplannersystem.mealplan.service.filter.RecipeFilterService;
import com.feex.mealplannersystem.mealplan.dto.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RecipeScorerService {

    public enum ScoringMode { FULL, ILP_MODE }

    public RecipeCandidateDto scoreRecipe(RecipeModel recipe, UserProfileModel user,
                                          double slotBudget, double scaledServings, double scaledCalories,
                                          Map<Integer, Map<String, List<Integer>>> usageBySlotType,
                                          int currentDay, MacroTarget macroTarget,
                                          RecipeDataContext data, ClassificationContext classification,
                                          List<String> relaxations, String slotType,
                                          int proteinWeight) {
        return scoreRecipe(recipe, user, slotBudget, scaledServings, scaledCalories,
                usageBySlotType, currentDay, macroTarget, data, classification,
                relaxations, slotType, proteinWeight, ScoringMode.FULL, 0);
    }

    public RecipeCandidateDto scoreRecipe(RecipeModel recipe, UserProfileModel user,
                                          double slotBudget, double scaledServings, double scaledCalories,
                                          Map<Integer, Map<String, List<Integer>>> usageBySlotType,
                                          int currentDay, MacroTarget macroTarget,
                                          RecipeDataContext data, ClassificationContext classification,
                                          List<String> relaxations, String slotType,
                                          int proteinWeight, ScoringMode mode, int poolSize) {

        NutritionModel nutrition = data.getNutrition(recipe.getId());
        double calsPerServing = nutrition.getCalories();

        double slotProteinTarget = macroTarget.proteinPerMealG;
        if (slotType != null && slotType.startsWith("snack")) slotProteinTarget *= 0.6;

        int calorieFit   = scoreCalorieFit(scaledCalories, slotBudget);
        int cookTimeScore = scoreCookTime(recipe.getCookTime(), recipe.getTags());

        int proteinFit = 0, carbsFit = 0, fatFit = 0;
        if (mode == ScoringMode.FULL) {
            proteinFit = scoreProteinFit(nutrition.getProteinG(), scaledServings, slotProteinTarget, proteinWeight);
            carbsFit   = scoreCarbsFit(nutrition.getTotalCarbsG() * scaledServings,
                    macroTarget.carbsPerMealG, macroTarget.carbsTargetG, recipe.getName(), recipe.getTags());
            fatFit     = scoreFatFit(nutrition.getTotalFatG() * scaledServings, macroTarget.fatPerMealG);
        }

        List<String> matchedHealthTags = new ArrayList<>();
        int nutritionQuality = scoreNutritionQuality(recipe.getTags(), user, matchedHealthTags,
                macroTarget.carbsTargetG);

        int variety        = scoreVariety(recipe.getId(), user, usageBySlotType, currentDay, slotType, poolSize);
        int vegProteinBonus = scoreVegProteinBonus(recipe, user);

        List<String> activeConditions = buildActiveConditions(user);
        Map<String, List<String>> softMatches = computeSoftForbidden(recipe, activeConditions, classification);
        int softPenalty = calcSoftPenalty(softMatches);

        ScoreBreakdownDto breakdown = new ScoreBreakdownDto(
                calorieFit, 0, 0, cookTimeScore,
                proteinFit, carbsFit, fatFit,
                nutritionQuality, variety, vegProteinBonus, softPenalty);

        double deviation = Math.round(((scaledCalories - slotBudget) / slotBudget * 100.0) * 10.0) / 10.0;


        List<String> thresholds = RecipeFilterService.getAppliedThresholds(
                user.getHealthConditions() != null ? user.getHealthConditions() : List.of());
        List<String> dietaryNotes = buildDietaryNotes(recipe, user, softMatches);

        double scaledProtein = nutrition.getProteinG() * scaledServings;
        double scaledCarbs   = nutrition.getTotalCarbsG() * scaledServings;
        double scaledFat     = nutrition.getTotalFatG() * scaledServings;

        return new RecipeCandidateDto(
                recipe.getId(), recipe.getName(), breakdown.total(),
                calsPerServing, scaledServings,
                Math.round(scaledCalories * 10.0) / 10.0, deviation,
                scaledProtein,
                scaledCarbs,
                scaledFat,
                matchedHealthTags, thresholds, dietaryNotes,
                relaxations, breakdown, recipe.getParsedIngredients());
    }

    private static int scoreCalorieFit(double scaledCalories, double budget) {
        if (budget <= 0) return 0;
        double dev = Math.abs(scaledCalories - budget) / budget;
        if (dev >= 0.30) return 0;
        return (int) Math.round(35.0 * (1.0 - dev / 0.30));
    }

    private static int scoreCookTime(String cookTime, List<String> tags) {
        if (cookTime == null) return 0;
        return switch (cookTime.toUpperCase().trim()) {
            case "15_MIN", "15 MIN" -> 10;
            case "30_MIN", "30 MIN" -> 8;
            case "60_MIN", "60 MIN" -> 6;
            case "4_HOURS", "4 HOURS" -> {
                if (tags != null && tags.stream().anyMatch(t -> "slow-cooker".equals(t) || "crock-pot".equals(t)))
                    yield 5;
                yield 0;
            }
            default -> 0;
        };
    }

    private static int scoreProteinFit(double proteinPerServing, double servings,
                                        double target, int maxScore) {
        if (target <= 0) return Math.round(maxScore * 0.5f);
        double ratio = (proteinPerServing * servings) / target;
        if (ratio >= 1.00) return maxScore;
        if (ratio >= 0.80) return Math.round(maxScore * 0.8f);
        if (ratio >= 0.60) return Math.round(maxScore * 0.5f);
        if (ratio >= 0.40) return Math.round(maxScore * 0.3f);
        if (ratio >= 0.20) return Math.round(maxScore * 0.1f);
        return 0;
    }

    private static int scoreCarbsFit(double carbsScaled, int carbsPerMealG,
                                      int dailyCarbsTargetG, String name, List<String> tags) {
        if (dailyCarbsTargetG > 150 && carbsScaled < 10) {
            String nl = name != null ? name.toLowerCase() : "";
            if (nl.contains("low carb") || nl.contains("keto") || nl.contains("zero carb")) return 0;
        }
        if (carbsPerMealG <= 0) return 5;
        double ratio = carbsScaled / (double) carbsPerMealG;
        if (ratio >= 0.85) return 10;
        if (ratio >= 0.60) return 7;
        if (ratio >= 0.35) return 4;
        if (ratio >= 0.15) return 2;
        return 0;
    }

    private static int scoreFatFit(double fatScaled, int fatPerMealG) {
        if (fatPerMealG <= 0) return 3;
        double ratio = fatScaled / (double) fatPerMealG;
        if (ratio >= 0.50 && ratio <= 1.50) return 5;
        if (ratio >= 0.30 && ratio <= 1.75) return 3;
        if (ratio > 1.75) return 0;
        return 1;
    }

    private static int scoreNutritionQuality(List<String> tags, UserProfileModel user,
                                              List<String> matchedHealthTags, int dailyCarbsTarget) {
        if (tags == null) return 0;
        int score = 0;
        List<String> conditions = user.getHealthConditions() != null ? user.getHealthConditions() : List.of();

        Set<String> healthTagsOfInterest = new HashSet<>();
        for (String c : conditions) healthTagsOfInterest.add(c.toLowerCase().replace("_", "-"));
        if (user.getDietType() != null && !"omnivore".equalsIgnoreCase(user.getDietType()))
            healthTagsOfInterest.add(user.getDietType().toLowerCase().replace("_", "-"));
        if (dailyCarbsTarget < 100) { healthTagsOfInterest.add("low-carb"); healthTagsOfInterest.add("keto"); }

        for (String tag : tags) {
            String t = tag.toLowerCase();
            if (healthTagsOfInterest.contains(t)) { score += 8; matchedHealthTags.add(tag); }
            else if (Set.of("high-fiber","low-sodium","low-fat","heart-healthy",
                    "diabetic-friendly","anti-inflammatory").contains(t)) { score += 3; matchedHealthTags.add(tag); }
            else if (Set.of("low-carb","keto","paleo","whole30").contains(t)) score += 2;
        }
        return Math.min(score, 20);
    }

    private static int scoreVariety(long recipeId, UserProfileModel user,
                                    Map<Integer, Map<String, List<Integer>>> usageBySlotType,
                                    int currentDay, String slotType, int poolSize) {
        if (usageBySlotType == null) return 10;
        Map<String, List<Integer>> slotMap = usageBySlotType.get((int) recipeId);
        if (slotMap == null) return 10;

        int totalUses = slotMap.values().stream().mapToInt(List::size).sum();
        if (totalUses == 0) return 10;

        int penalty = totalUses * (poolSize > 500 ? 8 : poolSize > 200 ? 6 : 4);
        return Math.max(0, 10 - penalty);
    }

    private static int scoreVegProteinBonus(RecipeModel recipe, UserProfileModel user) {
        String diet = user.getDietType() != null ? user.getDietType().toUpperCase() : "";
        if (!Set.of("VEGETARIAN","VEGAN","PLANT_BASED").contains(diet)) return 0;
        List<String> vegProtein = List.of("tofu","tempeh","lentil","chickpea","bean","beans",
                "edamame","seitan","quinoa","hemp seed","chia seed");
        for (String ing : recipe.getParsedIngredients())
            if (vegProtein.stream().anyMatch(ing.toLowerCase()::contains)) return 5;
        return 0;
    }

    private static int calcSoftPenalty(Map<String, List<String>> softMatchesByCondition) {
        int total = 0;
        Set<String> penalized = new HashSet<>();
        for (Map.Entry<String, List<String>> e : softMatchesByCondition.entrySet()) {
            int pp = switch (RecipeFilterService.getConditionSeverity(e.getKey())) {
                case CRITICAL -> 20; case HIGH -> 15; case MODERATE -> 10; default -> 5;
            };
            for (String match : e.getValue())
                if (penalized.add(e.getKey() + ":" + match.toLowerCase())) total += pp;
        }
        return total;
    }

    private static Map<String, List<String>> computeSoftForbidden(RecipeModel recipe,
                                                                    List<String> conditions,
                                                                    ClassificationContext ctx) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String cond : conditions) {
            List<String> matches = ctx.getSoftForbiddenMatches(recipe.getParsedIngredients(), cond);
            if (!matches.isEmpty()) result.put(cond, matches);
        }
        return result;
    }

    private static List<String> buildActiveConditions(UserProfileModel user) {
        List<String> c = new ArrayList<>();
        if (user.getHealthConditions() != null)
            user.getHealthConditions().forEach(h -> c.add(h.toLowerCase()));
        if (user.getDietType() != null && !"omnivore".equalsIgnoreCase(user.getDietType()))
            c.add(user.getDietType().toLowerCase());
        return c;
    }

    private static List<String> buildDietaryNotes(RecipeModel recipe, UserProfileModel user,
                                                   Map<String, List<String>> softMatches) {
        List<String> notes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        softMatches.values().forEach(list -> list.forEach(m -> {
            if (seen.add(m.toLowerCase())) notes.add("Contains acceptable amount of " + m);
        }));
        List<String> conditions = buildActiveConditions(user);
        if (conditions.contains("lactose_intolerance")) {
            boolean hasDairy = recipe.getParsedIngredients().stream().anyMatch(i -> {
                String s = i.toLowerCase();
                return s.contains("milk")||s.contains("cheese")||s.contains("cream")||
                        s.contains("butter")||s.contains("yogurt")||s.contains("dairy");
            });
            if (hasDairy) notes.add("Use lactose-free dairy alternatives");
        }
        if (conditions.contains("celiac_disease")) {
            boolean hasSoy = recipe.getParsedIngredients().stream().anyMatch(i ->
                    i.toLowerCase().contains("soy sauce") && !i.toLowerCase().contains("gluten-free"));
            if (hasSoy) notes.add("Use gluten-free soy sauce or tamari");
        }
        return notes;
    }
}
