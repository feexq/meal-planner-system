package com.feex.mealplannersystem.domain.nutrition; // Перевірте свій пакет!

import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserHealthConditionEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeIngredientEntity;
import com.feex.mealplannersystem.repository.entity.recipe.RecipeNutritionEntity;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import com.feex.mealplannersystem.domain.output.RecipeCandidate;
import com.feex.mealplannersystem.domain.output.ScoreBreakdown;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecipeScorer {

    private final ClassifiedIngredientsService classifiedIngredientsService;
    private final RecipeFilter recipeFilter;
    private final MacroRequirementCalculator macroRequirementCalculator;

    public RecipeScorer(ClassifiedIngredientsService classifiedIngredientsService,
                        RecipeFilter recipeFilter,
                        MacroRequirementCalculator macroRequirementCalculator) {
        this.classifiedIngredientsService = classifiedIngredientsService;
        this.recipeFilter = recipeFilter;
        this.macroRequirementCalculator = macroRequirementCalculator;
    }

    public enum ScoringMode { FULL, ILP_MODE }

    public RecipeCandidate scoreRecipe(RecipeEntity recipe, UserPreferenceEntity user,
                                       double slotBudget, double scaledServings, double scaledCalories,
                                       Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
                                       int currentDay, MacroTarget macroTarget,
                                       List<String> relaxations, String slotType, int proteinWeight, ScoringMode mode, int poolSize) {

        RecipeNutritionEntity nutrition = recipe.getNutrition();

        // БЕЗПЕЧНЕ ОТРИМАННЯ КАЛОРІЙ
        double caloriesPerServing = nutrition != null && nutrition.getCalories() != null
                ? nutrition.getCalories().doubleValue() : 0.0;

        double slotProteinTarget = macroTarget.proteinPerMealG;
        if (slotType != null && slotType.startsWith("snack")) {
            slotProteinTarget *= 0.6;
        }

        int calorieFit = scoreCalorieFit(scaledCalories, slotBudget);
        int complexityMatch = 0;
        int budgetMatch = 0;

        List<String> stringTags = recipe.getTags().stream().map(TagEntity::getName).collect(Collectors.toList());
        int cookTimeScore = scoreCookTime(recipe.getCookTime() != null ? recipe.getCookTime().name() : null, stringTags);

        int proteinFit = 0;
        if (mode == ScoringMode.FULL) {
            // БЕЗПЕЧНЕ ОТРИМАННЯ ПРОТЕЇНУ
            double recipeProteinPerServing = nutrition != null && nutrition.getProteinG() != null
                    ? nutrition.getProteinG().doubleValue() : 0.0;
            proteinFit = scoreProteinFit(recipeProteinPerServing, scaledServings, slotProteinTarget, proteinWeight);
        }

        List<String> matchedHealthTags = new ArrayList<>();
        int nutritionQuality = scoreNutritionQuality(stringTags, user, matchedHealthTags, macroTarget.carbsTargetG);

        int variety = scoreVariety(recipe.getId().intValue(), user, recipeUsageBySlotType, currentDay, slotType, poolSize);
        int vegProteinBonus = scoreVegProteinBonus(recipe, user);

        int carbsFit = 0;
        if (mode == ScoringMode.FULL) {
            // БЕЗПЕЧНЕ ОТРИМАННЯ ВУГЛЕВОДІВ
            double safeCarbs = nutrition != null && nutrition.getTotalCarbsG() != null ? nutrition.getTotalCarbsG().doubleValue() : 0.0;
            double recipeCarbsScaled = safeCarbs * scaledServings;
            carbsFit = macroRequirementCalculator.calcCarbsFit(recipeCarbsScaled, macroTarget.carbsPerMealG, macroTarget.carbsTargetG, recipe.getName());
        }

        int fatFit = 0;
        if (mode == ScoringMode.FULL) {
            // БЕЗПЕЧНЕ ОТРИМАННЯ ЖИРІВ
            double safeFat = nutrition != null && nutrition.getTotalFatG() != null ? nutrition.getTotalFatG().doubleValue() : 0.0;
            double recipeFatScaled = safeFat * scaledServings;
            fatFit = macroRequirementCalculator.calcFatFit(recipeFatScaled, macroTarget.fatPerMealG);
        }

        List<String> activeConditions = buildActiveConditions(user);
        Map<String, List<String>> softMatchesByCondition = computeSoftForbiddenMatches(recipe, activeConditions);
        int softForbiddenPenalty = calculateSoftForbiddenPenalty(softMatchesByCondition);

        ScoreBreakdown breakdown = new ScoreBreakdown(
                calorieFit, complexityMatch, budgetMatch, cookTimeScore, proteinFit, carbsFit, fatFit,
                nutritionQuality, variety, vegProteinBonus, softForbiddenPenalty);

        double deviation = slotBudget > 0 ? ((scaledCalories - slotBudget) / slotBudget) * 100.0 : 0.0;
        deviation = Math.round(deviation * 10.0) / 10.0;

        List<String> userConditions = user.getHealthConditions().stream()
                .map(UserHealthConditionEntity::getConditionName).collect(Collectors.toList());
        List<String> appliedThresholds = recipeFilter.getAppliedThresholds(userConditions);
        List<String> dietaryNotes = buildDietaryNotes(recipe, user, softMatchesByCondition);

        List<String> stringIngredients = recipe.getIngredients().stream()
                .map(RecipeIngredientEntity::getRawName).collect(Collectors.toList());

        return new RecipeCandidate(
                recipe.getId().intValue(), recipe.getName(), breakdown.total(), caloriesPerServing,
                scaledServings, Math.round(scaledCalories * 10.0) / 10.0, deviation,
                matchedHealthTags, appliedThresholds, dietaryNotes, relaxations, breakdown, stringIngredients);
    }

    private int scoreCalorieFit(double scaledCalories, double slotBudget) {
        if (slotBudget <= 0) return 0;
        double deviation = Math.abs(scaledCalories - slotBudget) / slotBudget;
        if (deviation <= 0.05) return 25;
        if (deviation <= 0.10) return 20;
        if (deviation <= 0.15) return 10;
        if (deviation <= 0.20) return 5;
        return 0;
    }

    private int scoreCookTime(String cookTime, List<String> tags) {
        int score = 0;
        if (cookTime != null) {
            switch (cookTime.toLowerCase()) {
                case "under 15 mins":
                    score += 10;
                    break;
                case "under 30 mins":
                    score += 8;
                    break;
                case "under 1 hour":
                    score += 5;
                    break;
                case "under 4 hours":
                    score += 2;
                    break;
                default:
                    score += 0;
            }
        }
        if (tags != null) {
            for (String tag : tags) {
                String t = tag.toLowerCase();
                if (t.contains("one-pot") || t.contains("sheet pan") || t.contains("no-cook")
                        || t.contains("easy") || t.contains("quick")) {
                    score += 3;
                }
            }
        }
        return Math.min(score, 15);
    }

    private int scoreProteinFit(double recipeProteinPerServing, double scaledServings, double slotProteinTarget, int proteinWeight) {
        if (slotProteinTarget <= 0) return 0;
        double scaledProtein = recipeProteinPerServing * scaledServings;
        double ratio = scaledProtein / slotProteinTarget;

        if (ratio >= 0.90 && ratio <= 1.25) return proteinWeight;
        if (ratio >= 0.75 && ratio <= 1.50) return (int) (proteinWeight * 0.7);
        if (ratio >= 0.50) return (int) (proteinWeight * 0.4);
        return 0;
    }

    private int scoreNutritionQuality(List<String> tags, UserPreferenceEntity user, List<String> matchedHealthTags, int carbsTargetG) {
        int score = 0;
        List<String> conditions = user.getHealthConditions().stream()
                .map(UserHealthConditionEntity::getConditionName)
                .map(String::toLowerCase).collect(Collectors.toList());

        if (tags == null) return 0;

        for (String t : tags) {
            String tagLower = t.toLowerCase();
            if (conditions.contains("diabetes") && carbsTargetG <= 0) {
                if (tagLower.contains("low-carb") || tagLower.contains("diabetic") || tagLower.contains("low-sugar")
                        || tagLower.contains("keto") || tagLower.contains("paleo")) {
                    score += 8;
                    matchedHealthTags.add(t);
                }
            }
            if (conditions.contains("hypertension")) {
                if (tagLower.contains("low-sodium") || tagLower.contains("heart-healthy") || tagLower.contains("dash")) {
                    score += 8;
                    matchedHealthTags.add(t);
                }
            }
            if (conditions.contains("high_cholesterol")) {
                if (tagLower.contains("low-fat") || tagLower.contains("heart-healthy") || tagLower.contains("mediterranean")) {
                    score += 8;
                    matchedHealthTags.add(t);
                }
            }
            if (conditions.contains("kidney_disease")) {
                if (tagLower.contains("kidney-friendly") || tagLower.contains("low-sodium") || tagLower.contains("low-protein")) {
                    score += 8;
                    matchedHealthTags.add(t);
                }
            }
            if (conditions.contains("gout")) {
                if (tagLower.contains("low-purine") || tagLower.contains("gout-friendly") || tagLower.contains("vegetarian")) {
                    score += 8;
                    matchedHealthTags.add(t);
                }
            }
            if (conditions.contains("celiac_disease") && tagLower.contains("gluten-free")) {
                matchedHealthTags.add(t);
            }
            if (conditions.contains("ibs") && tagLower.contains("low-fodmap")) {
                score += 8;
                matchedHealthTags.add(t);
            }
            if (conditions.contains("lactose_intolerance") && tagLower.contains("dairy-free")) {
                matchedHealthTags.add(t);
            }

            if (tagLower.contains("healthy") || tagLower.contains("superfood") || tagLower.contains("high-fiber")
                    || tagLower.contains("protein") || tagLower.contains("nutrient-dense") || tagLower.contains("whole30")) {
                score += 2;
            }
        }
        return Math.min(score, 15);
    }

    private int scoreVariety(int recipeId, UserPreferenceEntity user, Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType, int currentDay, String slotType, int poolSize) {
        int baseScore = 15;
        if (recipeUsageBySlotType == null || !recipeUsageBySlotType.containsKey(recipeId)) {
            return baseScore;
        }

        Map<String, List<Integer>> usage = recipeUsageBySlotType.get(recipeId);
        if (usage.isEmpty()) return baseScore;

        int totalUses = 0;
        int usesInSameSlot = 0;
        for (Map.Entry<String, List<Integer>> entry : usage.entrySet()) {
            totalUses += entry.getValue().size();
            if (entry.getKey().equals(slotType)) {
                usesInSameSlot += entry.getValue().size();
            }
        }

        int maxRepeats = 2;
        if (totalUses >= maxRepeats && poolSize > 20) {
            return -50;
        }

        int score = baseScore;
        score -= (totalUses * 5);
        score -= (usesInSameSlot * 5);

        List<Integer> daysUsed = new ArrayList<>();
        for (List<Integer> days : usage.values()) daysUsed.addAll(days);
        Collections.sort(daysUsed);

        if (!daysUsed.isEmpty()) {
            int lastUsed = daysUsed.get(daysUsed.size() - 1);
            if (currentDay == lastUsed) {
                score -= 10;
            } else if (currentDay - lastUsed == 1) {
                score -= 5;
            }
        }
        return Math.max(0, score);
    }

    private int scoreVegProteinBonus(RecipeEntity recipe, UserPreferenceEntity user) {
        String dietType = user.getDietType() != null ? user.getDietType().name().toUpperCase() : "OMNIVORE";
        if (!"VEGETARIAN".equals(dietType) && !"VEGAN".equals(dietType)) return 0;

        List<String> ingredients = recipe.getIngredients().stream()
                .map(RecipeIngredientEntity::getRawName).collect(Collectors.toList());
        boolean hasEgg = false, hasSoyPrio = false, hasLegumes = false, hasNutsSeeds = false;

        for (String ing : ingredients) {
            String s = ing.toLowerCase();
            if (s.matches(".*\\begg(s)?\\b.*")) hasEgg = true;
            if (s.contains("tofu") || s.contains("tempeh") || s.contains("edamame") || s.contains("soy")) hasSoyPrio = true;
            if (s.contains("lentil") || s.contains("bean") || s.contains("chickpea") || s.contains("pea")) hasLegumes = true;
            if (s.contains("almond") || s.contains("walnut") || s.contains("chia") || s.contains("hemp") || s.contains("flax")) hasNutsSeeds = true;
        }

        if ("VEGETARIAN".equals(dietType) && hasEgg) return 5;
        if (hasSoyPrio) return 5;
        if (hasLegumes) return 4;
        if (hasNutsSeeds) return 3;

        return 0;
    }

    private int calculateSoftForbiddenPenalty(Map<String, List<String>> softMatchesByCondition) {
        if (softMatchesByCondition.isEmpty()) return 0;
        int totalPenalty = 0;
        Set<String> alreadyPenalized = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : softMatchesByCondition.entrySet()) {
            String cond = entry.getKey();
            RecipeFilter.ConditionSeverity severity = recipeFilter.getConditionSeverity(cond);

            int penaltyPerMatch = switch (severity) {
                case CRITICAL -> 20;
                case HIGH -> 15;
                case MODERATE -> 10;
                default -> 5;
            };

            for (String match : entry.getValue()) {
                if (alreadyPenalized.add(cond + ":" + match.toLowerCase())) {
                    totalPenalty += penaltyPerMatch;
                }
            }
        }
        return totalPenalty;
    }

    private List<String> buildActiveConditions(UserPreferenceEntity user) {
        List<String> conditions = user.getHealthConditions().stream()
                .map(c -> c.getConditionName().toLowerCase()).collect(Collectors.toList());
        if (user.getDietType() != null && !"OMNIVORE".equalsIgnoreCase(user.getDietType().name())) {
            conditions.add(user.getDietType().name().toLowerCase());
        }
        return conditions;
    }

    private Map<String, List<String>> computeSoftForbiddenMatches(RecipeEntity recipe, List<String> conditions) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (conditions.isEmpty()) return result;

        for (String cond : conditions) {
            List<String> matches = classifiedIngredientsService.getSoftForbiddenMatches(recipe.getIngredients(), cond);
            if (!matches.isEmpty()) {
                result.put(cond, matches);
            }
        }
        return result;
    }

    private List<String> buildDietaryNotes(RecipeEntity recipe, UserPreferenceEntity user, Map<String, List<String>> softMatchesByCondition) {
        List<String> dietaryNotes = new ArrayList<>();
        List<String> conditions = buildActiveConditions(user);

        Set<String> processedItems = new HashSet<>();
        for (List<String> softMatches : softMatchesByCondition.values()) {
            for (String match : softMatches) {
                if (processedItems.add(match.toLowerCase())) {
                    dietaryNotes.add("Contains acceptable amount of " + match);
                }
            }
        }

        List<String> ings = recipe.getIngredients().stream().map(RecipeIngredientEntity::getRawName).collect(Collectors.toList());

        if (conditions.contains("lactose_intolerance")) {
            for (String s : ings) {
                s = s.toLowerCase();
                if (s.contains("milk") || s.contains("cheese") || s.contains("cream")
                        || s.contains("butter") || s.contains("yogurt") || s.contains("curd")
                        || s.contains("dairy") || s.contains("whey") || s.contains("kefir")) {
                    dietaryNotes.add("Use lactose-free dairy alternatives (milk, cheese, etc.)");
                    break;
                }
            }
        }

        if (conditions.contains("celiac_disease")) {
            for (String s : ings) {
                s = s.toLowerCase();
                if (s.contains("soy sauce") && !s.contains("gluten-free")) {
                    dietaryNotes.add("Use gluten-free soy sauce or tamari");
                    break;
                }
            }
        }

        return dietaryNotes;
    }
}