package service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Recipe;
import model.NutritionEntry;
import model.NutritionInfo;
import model.UserProfile;
import model.output.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MealPlanGenerator {

    private final DataLoader dataLoader;

    public MealPlanGenerator(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public WeeklyMealPlan generatePlan(UserProfile user) {
        double[] dailyTargets = CalorieCalculator.getDailyTargets(user);
        double baseTarget = CalorieCalculator.adjustForGoal(user);
        double weeklyTotal = CalorieCalculator.getWeeklyTotal(dailyTargets);

        Map<String, Double> slotRatios = CalorieCalculator.getSlotRatios(user.getMealsPerDay());

        Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType = new HashMap<>();

        MacroRequirementCalculator.MacroTarget macroTarget = MacroRequirementCalculator.calculateMacros(user,
                baseTarget);

        List<DayPlan> days = new ArrayList<>();

        for (int day = 0; day < 7; day++) {
            double dailyCalories = dailyTargets[day];
            List<MealSlot> slots = buildDay(day + 1, dailyCalories, slotRatios,
                    macroTarget, user, recipeUsageBySlotType);

            EstimatedDailyMacros macros = computeDailyMacros(slots, macroTarget);

            if ("CRITICAL".equals(macros.getProteinStatus())) {
                System.out.println("  [Day " + (day + 1) + " CRITICAL protein ("
                        + macros.getProteinCoveragePercent() + "%). Retrying with boosted protein...]");
                slots = regeneratedDayWithBoostedProtein(day + 1, dailyCalories,
                        slotRatios, macroTarget, user, recipeUsageBySlotType);
                macros = computeDailyMacros(slots, macroTarget);
            }

            if ("CRITICAL".equals(macros.getCarbsStatus())) {
                System.out.println("  [Day " + (day + 1) + " CRITICAL carbs ("
                        + macros.getCarbsCoveragePercent() + "%). Retrying with boosted carbs...]");
                slots = regeneratedDayWithBoostedCarbs(day + 1, dailyCalories,
                        slotRatios, macroTarget, user, recipeUsageBySlotType);
                macros = computeDailyMacros(slots, macroTarget);
            }

            int relaxedCount = 0;
            for (MealSlot slot : slots) {
                if (!slot.getCandidates().isEmpty()
                        && slot.getCandidates().get(0).getRelaxations().contains("mealTypeRelaxed")) {
                    relaxedCount++;
                }
            }
            double dayRelaxationRate = slots.isEmpty() ? 0 : (double) relaxedCount / slots.size();

            days.add(new DayPlan(day + 1, (int) Math.round(dailyCalories),
                    macros, slots, dayRelaxationRate));
        }

        int totalRelaxed = 0, totalSlots = 0;
        for (DayPlan dp : days) {
            for (MealSlot slot : dp.getSlots()) {
                totalSlots++;
                if (!slot.getCandidates().isEmpty()
                        && slot.getCandidates().get(0).getRelaxations().contains("mealTypeRelaxed")) {
                    totalRelaxed++;
                }
            }
        }
        double totalRelaxationRate = totalSlots > 0 ? (double) totalRelaxed / totalSlots : 0;
        boolean lowCoverageWarning = totalRelaxationRate > 0.3;

        return new WeeklyMealPlan(
                user.getUserId(),
                (int) Math.round(baseTarget),
                (int) Math.round(weeklyTotal),
                days, totalRelaxationRate, lowCoverageWarning);
    }

    private List<MealSlot> buildDay(int day, double dailyCalories,
            Map<String, Double> slotRatios,
            MacroRequirementCalculator.MacroTarget macroTarget,
            UserProfile user,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType) {

        List<MealSlot> slots = new ArrayList<>();
        Set<Integer> usedToday = new HashSet<>();

        double accumulatedProtein = 0;
        double accumulatedTarget = 0;

        for (Map.Entry<String, Double> slotEntry : slotRatios.entrySet()) {
            String slotName = slotEntry.getKey();
            double ratio = slotEntry.getValue();
            double slotBudget = dailyCalories * ratio;

            double baseSlotProtein = macroTarget.proteinTargetG * ratio;
            if (slotName.startsWith("snack"))
                baseSlotProtein *= 0.6;

            double deficit = accumulatedTarget - accumulatedProtein;
            double activeSlotProteinTarget = baseSlotProtein + (deficit > 0 ? deficit : 0);

            int remainingSlots = slotRatios.size() - slots.size();
            double proteinNeeded = macroTarget.proteinMinG - accumulatedProtein;
            double perRemaining = remainingSlots > 0 ? proteinNeeded / remainingSlots : 0;

            boolean forceProtein = false;
            int proteinWeightBoost = 0;
            if (perRemaining > 20.0) {
                proteinWeightBoost = 10;
                if (perRemaining > 35.0) {
                    forceProtein = true;
                    System.out.println("    [Rule A: High protein need ("
                            + Math.round(perRemaining) + "g/slot). Forcing protein-rich candidates.]");
                }
            }

            MealSlot slot = buildSlot(slotName, slotBudget, macroTarget,
                    user, usedToday, recipeUsageBySlotType, day,
                    forceProtein, false, proteinWeightBoost);
            slots.add(slot);

            accumulatedTarget += baseSlotProtein;
            if (!slot.getCandidates().isEmpty()) {
                RecipeCandidate top = slot.getCandidates().get(0);
                NutritionEntry n = dataLoader.getNutrition(top.getRecipeId());
                if (n != null) {
                    accumulatedProtein += n.getNutrition().getProteinG() * top.getRecommendedServings();
                }
            }
        }
        return slots;
    }

    private List<MealSlot> regeneratedDayWithBoostedProtein(int day, double dailyCalories,
            Map<String, Double> slotRatios,
            MacroRequirementCalculator.MacroTarget macroTarget,
            UserProfile user,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType) {

        List<MealSlot> slots = new ArrayList<>();
        Set<Integer> usedToday = new HashSet<>();
        double accProtein = 0, accTarget = 0;

        for (Map.Entry<String, Double> slotEntry : slotRatios.entrySet()) {
            String slotName = slotEntry.getKey();
            double ratio = slotEntry.getValue();
            double slotBudget = dailyCalories * ratio;

            double baseSlotProtein = macroTarget.proteinTargetG * ratio;
            double deficit = accTarget - accProtein;
            double activeTarget = baseSlotProtein + (deficit > 0 ? deficit : 0);

            MealSlot slot = buildSlot(slotName, slotBudget, macroTarget,
                    user, usedToday, recipeUsageBySlotType, day,
                    true, false, 25);
            slots.add(slot);

            accTarget += baseSlotProtein;
            if (!slot.getCandidates().isEmpty()) {
                RecipeCandidate top = slot.getCandidates().get(0);
                NutritionEntry n = dataLoader.getNutrition(top.getRecipeId());
                if (n != null)
                    accProtein += n.getNutrition().getProteinG() * top.getRecommendedServings();
            }
        }
        return slots;
    }

    private List<MealSlot> regeneratedDayWithBoostedCarbs(int day, double dailyCalories,
            Map<String, Double> slotRatios,
            MacroRequirementCalculator.MacroTarget macroTarget,
            UserProfile user,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType) {

        System.out.println("  [Day " + day + ": Carb-boost retry — excluding zero-carbsFit candidates]");
        List<MealSlot> slots = new ArrayList<>();
        Set<Integer> usedToday = new HashSet<>();

        for (Map.Entry<String, Double> slotEntry : slotRatios.entrySet()) {
            String slotName = slotEntry.getKey();
            double slotBudget = dailyCalories * slotEntry.getValue();

            MealSlot slot = buildSlot(slotName, slotBudget, macroTarget,
                    user, usedToday, recipeUsageBySlotType, day,
                    false, true, 0);
            slots.add(slot);

            if (!slot.getCandidates().isEmpty()) {
                usedToday.add(slot.getCandidates().get(0).getRecipeId());
            }
        }
        return slots;
    }

    private MealSlot buildSlot(String slotName, double slotBudget,
            MacroRequirementCalculator.MacroTarget macroTarget, 
            UserProfile user,
            Set<Integer> usedToday,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay,
            boolean forceProtein,
            boolean forceCarbs, 
            int proteinWeightBoost) {

        String userComplexity = user.getCookingComplexity().toUpperCase();
        String userBudget = user.getBudgetLevel().toUpperCase();

        RecipeFilter.FilterResult filterResult = RecipeFilter.filterRecipes(
                dataLoader.getRecipes(), user, dataLoader, slotName);
        List<Recipe> hardFiltered = filterResult.getRecipes();

        List<RecipeCandidate> candidates;

        candidates = buildCandidates(hardFiltered, slotBudget, macroTarget,
                user, usedToday, recipeUsageBySlotType, currentDay,
                false, false, false, slotName, forceProtein, forceCarbs, proteinWeightBoost);

        if (candidates.size() < 2 && !"ANY".equalsIgnoreCase(userComplexity)) {
            List<RecipeCandidate> relaxed = buildCandidates(hardFiltered, slotBudget, macroTarget,
                    user, usedToday, recipeUsageBySlotType, currentDay,
                    true, false, false, slotName, forceProtein, forceCarbs, proteinWeightBoost);
            mergeNewCandidates(candidates, relaxed);
        }

        if (candidates.size() < 2) {
            List<RecipeCandidate> relaxed = buildCandidates(hardFiltered, slotBudget, macroTarget,
                    user, usedToday, recipeUsageBySlotType, currentDay,
                    true, true, false, slotName, forceProtein, forceCarbs, proteinWeightBoost);
            mergeNewCandidates(candidates, relaxed);
        }

        if (candidates.size() < 2) {
            String effectiveMealType = slotName.equals("snack_2") ? "snack" : slotName;
            List<Recipe> adjacent = filterWithAdjacentMealTypes(user, effectiveMealType);
            List<RecipeCandidate> relaxed = buildCandidates(adjacent, slotBudget, macroTarget,
                    user, usedToday, recipeUsageBySlotType, currentDay,
                    true, true, true, slotName, forceProtein, forceCarbs, proteinWeightBoost);
            mergeNewCandidates(candidates, relaxed);
        }

        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (candidates.size() > 5)
            candidates = new ArrayList<>(candidates.subList(0, 5));

        boolean lowCoverage = candidates.size() < 2;
        String filteringNote = filterResult.getFilteringNote();
        if (lowCoverage && filteringNote == null) {
            int totalFiltered = hardFiltered.size();
            filteringNote = "Only " + candidates.size() + " candidates after all relaxations. "
                    + totalFiltered + " recipes passed hard filters.";
            Optional<Map.Entry<String, Integer>> topElim = filterResult.getEliminationCounts()
                    .entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .max(Map.Entry.comparingByValue());
            if (topElim.isPresent()) {
                filteringNote += " Top filter: " + topElim.get().getKey()
                        + " (" + topElim.get().getValue() + ")";
            }
        }

        if (!candidates.isEmpty()) {
            RecipeCandidate topPick = candidates.get(0);
            String effectiveSlotType = slotName.equals("snack_2") ? "snack" : slotName;
            usedToday.add(topPick.getRecipeId());
            recipeUsageBySlotType
                    .computeIfAbsent(topPick.getRecipeId(), k -> new HashMap<>())
                    .computeIfAbsent(effectiveSlotType, k -> new ArrayList<>())
                    .add(currentDay);
        }

        return new MealSlot(slotName, (int) Math.round(slotBudget),
                candidates, lowCoverage, filteringNote);
    }

    private List<RecipeCandidate> buildCandidates(List<Recipe> filteredRecipes,
            double slotBudget,
            MacroRequirementCalculator.MacroTarget macroTarget, 
            UserProfile user,
            Set<Integer> usedToday,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay,
            boolean relaxComplexity,
            boolean relaxBudget,
            boolean relaxMealType,
            String slotName,
            boolean forceProtein,
            boolean forceCarbs, 
            int proteinWeightBoost) {

        String userComplexity = user.getCookingComplexity().toUpperCase();
        String userBudget = user.getBudgetLevel().toUpperCase();
        String effectiveSlotType = slotName.equals("snack_2") ? "snack" : slotName;

        List<RecipeCandidate> candidates = new ArrayList<>();

        for (Recipe recipe : filteredRecipes) {
            NutritionEntry nutrition = dataLoader.getNutrition(recipe.getId());
            if (nutrition == null)
                continue;

            double caloriesPerServing = nutrition.getNutrition().getCalories();
            List<String> relaxations = new ArrayList<>();

            if (!"ANY".equalsIgnoreCase(userComplexity)) {
                if (RecipeFilter.exceedsComplexity(recipe.getCookComplexity(), userComplexity)) {
                    if (relaxComplexity && !RecipeFilter.exceedsComplexityByMoreThanOne(
                            recipe.getCookComplexity(), userComplexity)) {
                        relaxations.add("complexityRelaxed");
                    } else {
                        continue;
                    }
                }
            }

            if (RecipeFilter.exceedsBudget(recipe.getCookBudget(), userBudget)) {
                if (relaxBudget && !RecipeFilter.exceedsBudgetByMoreThanOne(
                        recipe.getCookBudget(), userBudget)) {
                    relaxations.add("budgetRelaxed");
                } else {
                    continue;
                }
            }

            if (relaxMealType) {
                String effectiveMealType = slotName.equals("snack_2") ? "snack" : slotName;
                String recipeMealType = recipe.getMealType();
                if (recipeMealType != null
                        && !recipeMealType.toLowerCase().equals(effectiveMealType)) {
                    if (!RecipeFilter.matchesAdjacentMealType(recipeMealType, effectiveMealType)) {
                        continue;
                    }
                    relaxations.add("mealTypeRelaxed");
                }
            }

            double scaledServings = Math.round((slotBudget / caloriesPerServing) * 10.0) / 10.0;
            scaledServings = Math.max(0.5, Math.min(1.5, scaledServings));
            double scaledCalories = caloriesPerServing * scaledServings;

            if (scaledCalories < slotBudget * 0.70 || scaledCalories > slotBudget * 1.30)
                continue;

            if (usedToday.contains(recipe.getId()))
                continue;

            int maxRepeats = user.getMaxRecipeRepeatsPerWeek() != null ? user.getMaxRecipeRepeatsPerWeek() : 2;
            Map<String, List<Integer>> slotMap = recipeUsageBySlotType.get(recipe.getId());
            if (slotMap != null) {
                int totalUsage = slotMap.values().stream().mapToInt(List::size).sum();
                if (totalUsage >= maxRepeats)
                    continue;
            }

            MacroRequirementCalculator.MacroTarget effectiveMacroTarget = macroTarget;

            int baseProteinWeight = 15 + proteinWeightBoost;

            RecipeCandidate candidate = RecipeScorer.scoreRecipe(
                    recipe, user, slotBudget, scaledServings, scaledCalories,
                    recipeUsageBySlotType, currentDay,
                    effectiveMacroTarget, 
                    dataLoader, relaxations, effectiveSlotType, baseProteinWeight);

            if (forceProtein && candidate.getScoreBreakdown().getProteinFit() == 0)
                continue;

            if (forceCarbs && candidate.getScoreBreakdown().getCarbsFit() == 0)
                continue;

            candidates.add(candidate);
        }

        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (candidates.size() > 5)
            return new ArrayList<>(candidates.subList(0, 5));
        return candidates;
    }

    private EstimatedDailyMacros computeDailyMacros(List<MealSlot> slots,
            MacroRequirementCalculator.MacroTarget target) {

        double totalProtein = 0, totalCarbs = 0, totalFat = 0;

        for (MealSlot slot : slots) {
            if (slot.getCandidates().isEmpty())
                continue;
            RecipeCandidate top = slot.getCandidates().get(0);
            NutritionEntry nutrition = dataLoader.getNutrition(top.getRecipeId());
            if (nutrition == null)
                continue;

            NutritionInfo n = nutrition.getNutrition();
            double servings = top.getRecommendedServings();
            totalProtein += n.getProteinG() * servings;
            totalCarbs += n.getTotalCarbsG() * servings;
            totalFat += n.getTotalFatG() * servings;
        }

        int proteinCoverage = target.proteinTargetG > 0
                ? (int) Math.round((totalProtein / target.proteinTargetG) * 100)
                : 0;
        String proteinStatus;
        if (proteinCoverage > 150)
            proteinStatus = "HIGH";
        else if (proteinCoverage >= 85)
            proteinStatus = "OK";
        else if (proteinCoverage >= 70)
            proteinStatus = "LOW";
        else
            proteinStatus = "CRITICAL";

        int carbsCoverage = target.carbsTargetG > 0
                ? (int) Math.round((totalCarbs / target.carbsTargetG) * 100)
                : 100;
        String carbsStatus = MacroRequirementCalculator.calcCarbsStatus(
                (int) Math.round(totalCarbs), target.carbsTargetG);

        String fatStatus = MacroRequirementCalculator.calcFatStatus(
                (int) Math.round(totalFat), target.fatMinG, target.fatMaxG);

        return new EstimatedDailyMacros(
                (int) Math.round(totalProtein),
                (int) Math.round(totalCarbs),
                (int) Math.round(totalFat),
                target.proteinTargetG,
                target.proteinMinG,
                target.proteinMaxG,
                target.fatTargetG,
                target.fatMinG,
                target.fatMaxG,
                target.carbsTargetG,
                target.carbsAbsoluteMinG,
                target.lowCarbWarning,
                proteinCoverage,
                proteinStatus,
                carbsCoverage, 
                carbsStatus, 
                fatStatus); 
    }

    private List<Recipe> filterWithAdjacentMealTypes(UserProfile user, String effectiveMealType) {
        return dataLoader.getRecipes().stream()
                .filter(r -> RecipeFilter.matchesAdjacentMealType(r.getMealType(), effectiveMealType))
                .filter(r -> dataLoader.getNutrition(r.getId()) != null)
                .filter(r -> passesHardFilters(r, user, dataLoader))
                .filter(r -> !RecipeFilter.isBannedFromMainMeal(r, effectiveMealType))
                .collect(Collectors.toList());
    }

    private boolean passesHardFilters(Recipe recipe, UserProfile user, DataLoader dl) {
        if (dl.getNutrition(recipe.getId()) == null)
            return false;
        if ("1+ days".equalsIgnoreCase(recipe.getCookTime()))
            return false;

        String userDiet = user.getDietType().toLowerCase();
        List<String> userConditions = user.getHealthConditions().stream()
                .map(String::toLowerCase).collect(Collectors.toList());
        List<String> userAllergies = user.getAllergies().stream()
                .map(String::toLowerCase).collect(Collectors.toList());
        List<String> userDisliked = new ArrayList<>(user.getDislikedIngredients().stream()
                .map(String::toLowerCase).collect(Collectors.toList()));

        if (userConditions.contains("gastritis")) {
            if (!userDisliked.contains("fried_food"))
                userDisliked.add("fried_food");
            if (!userDisliked.contains("spicy"))
                userDisliked.add("spicy");
        }

        if (!"omnivore".equals(userDiet)) {
            IngredientLookupService.IngredientLookupResult lookup = dl.getLookupResult(recipe.getId());
            if (lookup != null && lookup.isNotSuitableFor(userDiet))
                return false;
            ClassifiedIngredientsService cs = dl.getClassifiedIngredientsService();
            if (cs != null && cs.hasForbiddenIngredient(recipe.getParsedIngredients(), userDiet))
                return false;
        }

        if (userConditions.contains("celiac_disease") && RecipeFilter.containsGlutenIngredient(recipe))
            return false;

        IngredientLookupService.IngredientLookupResult lookup = dl.getLookupResult(recipe.getId());
        if (lookup != null) {
            for (String condition : userConditions) {
                if (lookup.hasContraindication(condition))
                    return false;
            }
        }

        ClassifiedIngredientsService cs = dl.getClassifiedIngredientsService();
        if (cs != null) {
            for (String condition : userConditions) {
                if (cs.hasForbiddenIngredient(recipe.getParsedIngredients(), condition))
                    return false;
            }
        }

        if (RecipeFilter.hasAllergyViolation(recipe, userAllergies))
            return false;
        if (RecipeFilter.hasDislikedIngredient(recipe, userDisliked))
            return false;

        return true;
    }

    private void mergeNewCandidates(List<RecipeCandidate> existing,
            List<RecipeCandidate> newOnes) {
        Set<Integer> existingIds = existing.stream()
                .map(RecipeCandidate::getRecipeId)
                .collect(Collectors.toSet());
        for (RecipeCandidate c : newOnes) {
            if (!existingIds.contains(c.getRecipeId())) {
                existing.add(c);
                existingIds.add(c.getRecipeId());
            }
        }
    }

    public static void writeOutput(WeeklyMealPlan plan, String outputDir, int index)
            throws IOException {
        Path dir = Paths.get(outputDir);
        Files.createDirectories(dir);

        String filename = "meal_plan_" + plan.getUserId() + "_" + index + ".json";
        Path outputPath = dir.resolve(filename);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(plan);

        Files.write(outputPath, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("  Written: " + outputPath.toAbsolutePath());
    }
}