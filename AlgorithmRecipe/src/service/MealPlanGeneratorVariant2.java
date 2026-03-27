package service;

import model.NutritionEntry;
import model.Recipe;
import model.UserProfile;
import model.output.*;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MealPlanGeneratorVariant2 {

    private final DataLoader dataLoader;

    private static final int TOP_N_CANDIDATES = 500;

    private static final int BASE_PROTEIN_WEIGHT = 15;

    public MealPlanGeneratorVariant2(DataLoader dataLoader) {
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

            System.out.println("  [Day " + (day + 1) + ": Phase 1 — Full Scoring | Phase 2 — ILP Solver]");
            List<MealSlot> slots = buildDayViaILP(
                    day + 1, dailyCalories, slotRatios, macroTarget, user, recipeUsageBySlotType);

            EstimatedDailyMacros macros = computeDailyMacros(slots, macroTarget);

            if ("CRITICAL".equals(macros.getProteinStatus())) {
                System.out.println("    ⚠ WARNING: Day " + (day + 1)
                        + " protein still CRITICAL after ILP (" + macros.getProteinCoveragePercent() + "%)."
                        + " Consider increasing TOP_N_CANDIDATES or checking recipe pool.");
            }
            if ("CRITICAL".equals(macros.getCarbsStatus())) {
                System.out.println("    ⚠ WARNING: Day " + (day + 1)
                        + " carbs still CRITICAL after ILP (" + macros.getCarbsCoveragePercent() + "%).");
            }

            int relaxedCount = 0;
            for (MealSlot slot : slots) {
                if (!slot.getCandidates().isEmpty()
                        && slot.getCandidates().get(0).getRelaxations().contains("mealTypeRelaxed")) {
                    relaxedCount++;
                }
            }
            double dayRelaxationRate = slots.isEmpty() ? 0 : (double) relaxedCount / slots.size();

            days.add(new DayPlan(day + 1, (int) Math.round(dailyCalories), macros, slots, dayRelaxationRate));
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

    private List<MealSlot> buildDayViaILP(int day, double dailyCalories,
            Map<String, Double> slotRatios,
            MacroRequirementCalculator.MacroTarget macroTarget,
            UserProfile user,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType) {

        Map<String, List<RecipeCandidate>> slotCandidates = new LinkedHashMap<>();
        Map<String, Double> slotBudgets = new LinkedHashMap<>();

        Set<Integer> usedToday = new HashSet<>();

        for (Map.Entry<String, Double> slotEntry : slotRatios.entrySet()) {
            String slotName = slotEntry.getKey();
            double slotBudget = dailyCalories * slotEntry.getValue();
            slotBudgets.put(slotName, slotBudget);

            List<RecipeCandidate> candidates = getTopCandidatesForSlot(
                    slotName, slotBudget, macroTarget, user,
                    usedToday, recipeUsageBySlotType, day);
            slotCandidates.put(slotName, candidates);

            if ("snack".equals(slotName) && candidates.size() > 0) {
                int excludeCount = Math.min(50, candidates.size());
                for (int i = 0; i < excludeCount; i++) {
                    usedToday.add(candidates.get(i).getRecipeId());
                }
            }

            System.out.println("    Slot '" + slotName + "': " + candidates.size() + " candidates after full scoring");
        }

        for (Map.Entry<String, List<RecipeCandidate>> entry : slotCandidates.entrySet()) {
            if (entry.getValue().isEmpty()) {
                System.out.println("    !! Slot '" + entry.getKey()
                        + "' has 0 candidates — ILP cannot run. Returning empty fallback.");
                List<MealSlot> emptySlots = new ArrayList<>();
                for (String slotName : slotRatios.keySet()) {
                    emptySlots.add(new MealSlot(
                            slotName,
                            (int) Math.round(dailyCalories * slotRatios.get(slotName)),
                            new ArrayList<>(), true,
                            "No candidates after filtering — slot '" + slotName + "' empty"));
                }
                return emptySlots;
            }
        }

        double tolerance = 0.0;
        while (tolerance <= 0.60) {
            ExpressionsBasedModel model = new ExpressionsBasedModel();

            Map<String, Map<String, Variable>> vars = new HashMap<>();

            for (String slotName : slotCandidates.keySet()) {
                Map<String, Variable> recipeVars = new HashMap<>();

                Expression slotExpr = model.addExpression("Slot_" + slotName).lower(1).upper(1);

                for (RecipeCandidate candidate : slotCandidates.get(slotName)) {
                    String varId = slotName + "_" + candidate.getRecipeId();

                    Variable x = model.addVariable(varId).binary().weight(candidate.getScore());

                    slotExpr.set(x, 1);
                    recipeVars.put(String.valueOf(candidate.getRecipeId()), x);
                }
                vars.put(slotName, recipeVars);
            }

            double pMin = Math.max(0, macroTarget.proteinMinG * (1.0 - tolerance));
            double pMax = macroTarget.proteinMaxG * (1.0 + tolerance);
            double cMin = Math.max(0, macroTarget.carbsAbsoluteMinG * (1.0 - tolerance));
            double cMax = 9999.0; 
            double fMin = Math.max(0, macroTarget.fatMinG * (1.0 - tolerance));
            double fMax = macroTarget.fatMaxG * (1.0 + tolerance);
            double calMin = dailyCalories * (0.85 - (tolerance / 2));
            double calMax = dailyCalories * (1.15 + (tolerance / 2));

            Expression protExpr = model.addExpression("Protein").lower(pMin).upper(pMax);
            Expression carbsExpr = model.addExpression("Carbs").lower(cMin).upper(cMax);
            Expression fatExpr = model.addExpression("Fat").lower(fMin).upper(fMax);
            Expression calExpr = model.addExpression("Calories").lower(calMin).upper(calMax);

            for (String slotName : slotCandidates.keySet()) {
                for (RecipeCandidate candidate : slotCandidates.get(slotName)) {
                    Variable x = vars.get(slotName).get(String.valueOf(candidate.getRecipeId()));

                    NutritionEntry n = dataLoader.getNutrition(candidate.getRecipeId());
                    double servings = candidate.getRecommendedServings();

                    double p = n.getNutrition().getProteinG() * servings;
                    double c = n.getNutrition().getTotalCarbsG() * servings;
                    double f = n.getNutrition().getTotalFatG() * servings;
                    double cals = candidate.getScaledCalories();

                    protExpr.set(x, p);
                    carbsExpr.set(x, c);
                    fatExpr.set(x, f);
                    calExpr.set(x, cals);
                }
            }

            model.options.time_abort = 2000; 
            Optimisation.Result result = model.maximise();

            if (result.getState().isFeasible()) {
                if (tolerance > 0) {
                    System.out.println("    -> ILP solved with relaxed constraints (tolerance: "
                            + Math.round(tolerance * 100) + "%)");
                } else {
                    System.out.println("    -> ILP solved with strict constraints");
                }

                List<MealSlot> finalSlots = buildFinalSlots(
                        slotCandidates, slotBudgets, vars, recipeUsageBySlotType, day);

                finalSlots.sort(Comparator.comparingInt(s -> slotOrder(s.getMealType())));

                return finalSlots;
            }

            tolerance += 0.05;
            if (tolerance <= 0.60) {
                System.out.println("    -> ILP infeasible at tolerance="
                        + Math.round((tolerance - 0.05) * 100) + "%, retrying with "
                        + Math.round(tolerance * 100) + "%...");
            }
        }

        System.out.println("    !! ILP FAILED: Could not find feasible solution even at 60% tolerance."
                + " Check recipe pool size and filter settings.");
        List<MealSlot> emptySlots = new ArrayList<>();
        for (String slotName : slotRatios.keySet()) {
            emptySlots.add(new MealSlot(
                    slotName,
                    (int) Math.round(dailyCalories * slotRatios.get(slotName)),
                    new ArrayList<>(), true, "ILP Failed — insufficient recipe pool"));
        }
        return emptySlots;
    }

    private List<RecipeCandidate> getTopCandidatesForSlot(
            String slotName, double slotBudget,
            MacroRequirementCalculator.MacroTarget macroTarget,
            UserProfile user,
            Set<Integer> usedToday,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay) {

        String userComplexity = user.getCookingComplexity().toUpperCase();
        String userBudget = user.getBudgetLevel().toUpperCase();
        String effectiveSlotType = slotName.equals("snack_2") ? "snack" : slotName;

        RecipeFilter.FilterResult fRes = RecipeFilter.filterRecipes(
                dataLoader.getRecipes(), user, dataLoader, effectiveSlotType);
        List<Recipe> filteredRecipes = fRes.getRecipes();

        boolean relaxMealType = false;
        if (filteredRecipes.size() < 10) {
            relaxMealType = true;
            fRes = RecipeFilter.filterRecipes(dataLoader.getRecipes(), user, dataLoader, "ALL");
            filteredRecipes = fRes.getRecipes();
            System.out.println("    Slot '" + slotName + "': <10 recipes after strict filter, relaxing meal type"
                    + " (pool after relax: " + filteredRecipes.size() + ")");
        }

        boolean debugFilter = "true".equalsIgnoreCase(System.getProperty("debug.filter"));
        if (debugFilter) {
            System.out.println("    [DEBUG] After strict filter slot='" + slotName
                    + "' pool=" + filteredRecipes.size());
            if (filteredRecipes.size() < 30) {
                RecipeFilter.debugFilterBreakdown(dataLoader.getRecipes(), user, dataLoader, effectiveSlotType);
            }
        }

        List<RecipeCandidate> candidates = new ArrayList<>();

        for (Recipe recipe : filteredRecipes) {
            NutritionEntry nutrition = dataLoader.getNutrition(recipe.getId());
            if (nutrition == null)
                continue;

            double caloriesPerServing = nutrition.getNutrition().getCalories();
            if (caloriesPerServing <= 0)
                continue;

            List<String> relaxations = new ArrayList<>();

            if (relaxMealType) {
                String recipeMealType = recipe.getMealType();
                if (recipeMealType != null && !recipeMealType.toLowerCase().equals(effectiveSlotType)) {
                    if (!RecipeFilter.matchesAdjacentMealType(recipeMealType, effectiveSlotType)) {
                        continue;
                    }
                    relaxations.add("mealTypeRelaxed");
                }
            }

            double scaledServings = Math.round((slotBudget / caloriesPerServing) * 10.0) / 10.0;
            scaledServings = Math.max(0.5, Math.min(1.5, scaledServings));
            double scaledCalories = caloriesPerServing * scaledServings;

            if (scaledCalories < slotBudget * 0.60 || scaledCalories > slotBudget * 1.40)
                continue;

            if (usedToday.contains(recipe.getId()))
                continue;

            int maxRepeats = user.getMaxRecipeRepeatsPerWeek() != null
                    ? user.getMaxRecipeRepeatsPerWeek()
                    : 2;
            Map<String, List<Integer>> slotMap = recipeUsageBySlotType.get(recipe.getId());
            if (slotMap != null) {
                int totalUsage = slotMap.values().stream().mapToInt(List::size).sum();
                if (totalUsage >= maxRepeats)
                    continue;
            }

            final int MIN_SAME_SLOT_GAP_DAYS = 5; 
            final int MIN_GLOBAL_GAP_DAYS = 3; 
            boolean gapViolation = false;

            if (slotMap != null) {

                List<Integer> sameSlotUsage = slotMap.get(effectiveSlotType);
                if (sameSlotUsage != null && !sameSlotUsage.isEmpty()) {
                    int lastUsedSameSlotDay = Collections.max(sameSlotUsage);
                    if (currentDay - lastUsedSameSlotDay < MIN_SAME_SLOT_GAP_DAYS) {
                        gapViolation = true;
                    }
                }

                int lastUsedAnyDay = -1;
                for (List<Integer> daysUsed : slotMap.values()) {
                    for (int d : daysUsed) {
                        if (d > lastUsedAnyDay)
                            lastUsedAnyDay = d;
                    }
                }
                if (lastUsedAnyDay != -1 && (currentDay - lastUsedAnyDay) < MIN_GLOBAL_GAP_DAYS) {
                    gapViolation = true;
                }
            }

            if (gapViolation) {
                if (filteredRecipes.size() >= 800) {

                    continue;
                } else if (filteredRecipes.size() >= 300) {

                    if (candidates.size() >= 15) {
                        continue;
                    }
                }

                relaxations.add("gapRelaxed");
            }

            RecipeCandidate candidate = RecipeScorer.scoreRecipe(
                    recipe, user, slotBudget, scaledServings, scaledCalories,
                    recipeUsageBySlotType, currentDay,
                    macroTarget, dataLoader,
                    relaxations,
                    effectiveSlotType, 
                    BASE_PROTEIN_WEIGHT, 
                    RecipeScorer.ScoringMode.FULL,
                    filteredRecipes.size()); 

            candidates.add(candidate);
        }

        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        if (candidates.size() > TOP_N_CANDIDATES) {
            return new ArrayList<>(candidates.subList(0, TOP_N_CANDIDATES));
        }
        return candidates;
    }

    private List<MealSlot> buildFinalSlots(
            Map<String, List<RecipeCandidate>> slotCandidates,
            Map<String, Double> slotBudgets,
            Map<String, Map<String, Variable>> vars,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int day) {

        List<MealSlot> finalSlots = new ArrayList<>();

        Set<Integer> usedToday = new HashSet<>();

        for (String slotName : slotCandidates.keySet()) {
            List<RecipeCandidate> chosenCandidates = new ArrayList<>();
            RecipeCandidate primaryCandidate = null;

            for (RecipeCandidate candidate : slotCandidates.get(slotName)) {
                Variable x = vars.get(slotName).get(String.valueOf(candidate.getRecipeId()));
                if (x != null && x.getValue().doubleValue() > 0.5) {
                    primaryCandidate = candidate;
                    chosenCandidates.add(candidate);

                    usedToday.add(candidate.getRecipeId());
                    String effectiveSlot = slotName.equals("snack_2") ? "snack" : slotName;
                    recipeUsageBySlotType
                            .computeIfAbsent(candidate.getRecipeId(), k -> new HashMap<>())
                            .computeIfAbsent(effectiveSlot, k -> new ArrayList<>())
                            .add(day);
                    break;
                }
            }

            if (primaryCandidate != null) {
                NutritionEntry primaryN = dataLoader.getNutrition(primaryCandidate.getRecipeId());
                if (primaryN != null) {
                    double pCals = primaryCandidate.getScaledCalories();
                    double pProtein = primaryN.getNutrition().getProteinG()
                            * primaryCandidate.getRecommendedServings();

                    List<RecipeCandidate> alternatives = new ArrayList<>();
                    for (RecipeCandidate candidate : slotCandidates.get(slotName)) {
                        if (candidate.getRecipeId() == primaryCandidate.getRecipeId())
                            continue;

                        NutritionEntry candidateN = dataLoader.getNutrition(candidate.getRecipeId());
                        if (candidateN == null)
                            continue;

                        double cCals = candidate.getScaledCalories();
                        double cProtein = candidateN.getNutrition().getProteinG()
                                * candidate.getRecommendedServings();

                        boolean calClose = Math.abs(cCals - pCals) / Math.max(1, pCals) <= 0.15;
                        boolean protClose = Math.abs(cProtein - pProtein) / Math.max(1, pProtein) <= 0.20;
                        if (calClose && protClose) {
                            alternatives.add(candidate);
                        }
                    }

                    alternatives.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
                    int needed = 4; 
                    for (RecipeCandidate alt : alternatives) {
                        if (needed-- <= 0)
                            break;
                        chosenCandidates.add(alt);
                    }
                }

                if (chosenCandidates.size() < 3) {
                    Set<Integer> alreadyIn = new HashSet<>();
                    for (RecipeCandidate c : chosenCandidates)
                        alreadyIn.add(c.getRecipeId());

                    for (RecipeCandidate candidate : slotCandidates.get(slotName)) {
                        if (chosenCandidates.size() >= 5)
                            break;
                        if (!alreadyIn.contains(candidate.getRecipeId())) {
                            chosenCandidates.add(candidate);
                            alreadyIn.add(candidate.getRecipeId());
                        }
                    }
                }
            }

            finalSlots.add(new MealSlot(
                    slotName,
                    (int) Math.round(slotBudgets.get(slotName)),
                    chosenCandidates,
                    false,
                    "Decided by ILP (Two-Phase v10)"));
        }

        return finalSlots;
    }

    private EstimatedDailyMacros computeDailyMacros(List<MealSlot> slots,
            MacroRequirementCalculator.MacroTarget macroTarget) {

        double protein = 0, carbs = 0, fat = 0;
        for (MealSlot slot : slots) {
            if (!slot.getCandidates().isEmpty()) {
                RecipeCandidate top = slot.getCandidates().get(0);
                NutritionEntry n = dataLoader.getNutrition(top.getRecipeId());
                if (n != null) {
                    double srv = top.getRecommendedServings();
                    protein += n.getNutrition().getProteinG() * srv;
                    carbs += n.getNutrition().getTotalCarbsG() * srv;
                    fat += n.getNutrition().getTotalFatG() * srv;
                }
            }
        }

        int pRounded = (int) Math.round(protein);
        int cRounded = (int) Math.round(carbs);
        int fRounded = (int) Math.round(fat);

        int pCov = macroTarget.proteinTargetG > 0
                ? (int) Math.round((protein / macroTarget.proteinTargetG) * 100)
                : 0;
        String pStatus;
        if (pCov > 150)
            pStatus = "HIGH";
        else if (pCov >= 85)
            pStatus = "OK";
        else if (pCov >= 70)
            pStatus = "LOW";
        else
            pStatus = "CRITICAL";

        String cStatus = MacroRequirementCalculator.calcCarbsStatus(cRounded, macroTarget.carbsTargetG);
        int cCov = macroTarget.carbsTargetG > 0
                ? (int) Math.round((carbs / macroTarget.carbsTargetG) * 100)
                : 0;

        String fStatus = MacroRequirementCalculator.calcFatStatus(fRounded, macroTarget.fatMinG, macroTarget.fatMaxG);
        int fCov = macroTarget.fatTargetG > 0
                ? (int) Math.round((fat / macroTarget.fatTargetG) * 100)
                : 0;

        return new EstimatedDailyMacros(
                pRounded, cRounded, fRounded,
                macroTarget.proteinTargetG, macroTarget.proteinMinG, macroTarget.proteinMaxG,
                macroTarget.fatTargetG, macroTarget.fatMinG, macroTarget.fatMaxG,
                macroTarget.carbsTargetG, macroTarget.carbsAbsoluteMinG, macroTarget.lowCarbWarning,
                pCov, pStatus,
                cCov, cStatus,
                fCov, fStatus);
    }

    private static int slotOrder(String mealType) {
        switch (mealType) {
            case "breakfast":
                return 1;
            case "lunch":
                return 2;
            case "dinner":
                return 3;
            case "snack":
                return 4;
            case "snack_2":
                return 5;
            default:
                return 99;
        }
    }

    public static void writeOutput(WeeklyMealPlan plan, String outputDir, int index) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(plan);
        Path outPath = Paths.get(outputDir, "meal_plan_" + plan.getUserId() + "_" + index + ".json");
        Files.createDirectories(outPath.getParent());
        Files.write(outPath, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("  Written: " + outPath.toAbsolutePath());
    }
}