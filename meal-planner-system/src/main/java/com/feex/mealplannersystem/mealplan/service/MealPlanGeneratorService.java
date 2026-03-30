package com.feex.mealplannersystem.mealplan.service;

import com.feex.mealplannersystem.mealplan.dto.*;
import com.feex.mealplannersystem.mealplan.dto.MealPlanDtos.*;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.calculator.CalorieCalculatorService;
import com.feex.mealplannersystem.mealplan.service.calculator.MacroRequirementService;
import com.feex.mealplannersystem.mealplan.service.calculator.MacroRequirementService.MacroTarget;
import com.feex.mealplannersystem.mealplan.service.filter.RecipeFilterService;
import com.feex.mealplannersystem.mealplan.service.scorer.RecipeScorerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Spring port of MealPlanGeneratorVariant2.
 *
 * Flow:
 *  1. Load all recipes + nutrition from DB (RecipeDataAdapter)
 *  2. Load ingredient classification data (IngredientClassificationAdapter)
 *  3. Convert UserPreferenceEntity → UserProfileModel (caller's responsibility via UserProfileAdapter)
 *  4. For each of 7 days: filter → score → ILP select
 *  5. Return WeeklyMealPlanDto
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanGeneratorService {

    private final RecipeDataAdapter          recipeDataAdapter;
    private final IngredientClassificationAdapter classificationAdapter;
    private final CalorieCalculatorService   calorieCalculator;
    private final MacroRequirementService    macroCalculator;
    private final RecipeFilterService        recipeFilter;
    private final RecipeScorerService        recipeScorer;

    private static final int TOP_N_CANDIDATES   = 500;
    private static final int BASE_PROTEIN_WEIGHT = 15;

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    public WeeklyMealPlanDto generatePlan(UserProfileModel user) {
        RecipeDataContext      data           = recipeDataAdapter.buildContext();
        ClassificationContext  classification = classificationAdapter.buildContext();

        double[] dailyTargets = calorieCalculator.getDailyTargets(user);
        double   baseTarget   = calorieCalculator.adjustForGoal(user);
        double   weeklyTotal  = calorieCalculator.getWeeklyTotal(dailyTargets);

        Map<String, Double> slotRatios = calorieCalculator.getSlotRatios(user.getMealsPerDay());
        MacroTarget macroTarget        = macroCalculator.calculateMacros(user, baseTarget);

        // recipeId → slotType → [day numbers used]
        Map<Integer, Map<String, List<Integer>>> usageBySlotType = new HashMap<>();

        List<DayPlanDto> days = new ArrayList<>();

        for (int d = 0; d < 7; d++) {
            double dailyCalories = dailyTargets[d];
            log.debug("Day {}: building via ILP (target={} kcal)", d + 1, (int) dailyCalories);

            List<MealSlotDto> slots = buildDayViaILP(
                    d + 1, dailyCalories, slotRatios, macroTarget,
                    user, data, classification, usageBySlotType);

            EstimatedDailyMacrosDto macros = computeDailyMacros(slots, macroTarget, data);

            if ("CRITICAL".equals(macros.getProteinStatus()))
                log.warn("Day {} protein CRITICAL ({}%)", d + 1, macros.getProteinCoveragePercent());
            if ("CRITICAL".equals(macros.getCarbsStatus()))
                log.warn("Day {} carbs CRITICAL ({}%)", d + 1, macros.getCarbsCoveragePercent());

            int relaxed = (int) slots.stream()
                    .filter(s -> !s.getCandidates().isEmpty() &&
                            s.getCandidates().get(0).getRelaxations().contains("mealTypeRelaxed"))
                    .count();
            double dayRelaxRate = slots.isEmpty() ? 0 : (double) relaxed / slots.size();

            days.add(new DayPlanDto(d + 1, (int) Math.round(dailyCalories), macros, slots, dayRelaxRate));
        }

        int totalSlots = days.stream().mapToInt(dp -> dp.getSlots().size()).sum();
        int totalRelaxed = days.stream().flatMap(dp -> dp.getSlots().stream())
                .filter(s -> !s.getCandidates().isEmpty() &&
                        s.getCandidates().get(0).getRelaxations().contains("mealTypeRelaxed"))
                .mapToInt(s -> 1).sum();
        double relaxRate = totalSlots > 0 ? (double) totalRelaxed / totalSlots : 0;

        return new WeeklyMealPlanDto(
                user.getUserId(),
                (int) Math.round(baseTarget),
                (int) Math.round(weeklyTotal),
                relaxRate,
                relaxRate > 0.3,
                days);
    }

    // -----------------------------------------------------------------------
    // Day builder (ILP with progressive tolerance relaxation)
    // -----------------------------------------------------------------------

    private List<MealSlotDto> buildDayViaILP(
            int day, double dailyCalories,
            Map<String, Double> slotRatios,
            MacroTarget macroTarget,
            UserProfileModel user,
            RecipeDataContext data,
            ClassificationContext classification,
            Map<Integer, Map<String, List<Integer>>> usageBySlotType) {

        Map<String, List<RecipeCandidateDto>> slotCandidates = new LinkedHashMap<>();
        Map<String, Double>                   slotBudgets    = new LinkedHashMap<>();
        Set<Integer>                          usedToday      = new HashSet<>();

        for (Map.Entry<String, Double> entry : slotRatios.entrySet()) {
            String slotName   = entry.getKey();
            double slotBudget = dailyCalories * entry.getValue();
            slotBudgets.put(slotName, slotBudget);

            List<RecipeCandidateDto> candidates = getTopCandidatesForSlot(
                    slotName, slotBudget, macroTarget, user,
                    data, classification, usedToday, usageBySlotType, day);
            slotCandidates.put(slotName, candidates);

            // pre-exclude top snack recipes so lunch/dinner don't repeat them
            if ("snack".equals(slotName)) {
                candidates.stream().limit(50).forEach(c -> usedToday.add((int) c.getRecipeId()));
            }

            log.debug("  Slot '{}': {} candidates", slotName, candidates.size());
        }

        // Guard: if any slot is empty, ILP can't run
        for (Map.Entry<String, List<RecipeCandidateDto>> e : slotCandidates.entrySet()) {
            if (e.getValue().isEmpty()) {
                log.warn("Slot '{}' has 0 candidates — returning empty fallback", e.getKey());
                return emptyFallback(slotRatios, dailyCalories, e.getKey());
            }
        }

        // ILP with progressive tolerance
        double tolerance = 0.0;
        while (tolerance <= 0.60) {
            ExpressionsBasedModel model = new ExpressionsBasedModel();
            Map<String, Map<String, Variable>> vars = new HashMap<>();

            for (String slotName : slotCandidates.keySet()) {
                Map<String, Variable> recipeVars = new HashMap<>();
                Expression slotExpr = model.addExpression("Slot_" + slotName).lower(1).upper(1);

                for (RecipeCandidateDto c : slotCandidates.get(slotName)) {
                    String varId = slotName + "_" + c.getRecipeId();
                    Variable x = model.addVariable(varId).binary().weight(c.getScore());
                    slotExpr.set(x, 1);
                    recipeVars.put(String.valueOf(c.getRecipeId()), x);
                }
                vars.put(slotName, recipeVars);
            }

            // macro constraints with current tolerance
            double pMin = Math.max(0, macroTarget.proteinMinG     * (1.0 - tolerance));
            double pMax =             macroTarget.proteinMaxG     * (1.0 + tolerance);
            double cMin = Math.max(0, macroTarget.carbsAbsoluteMinG * (1.0 - tolerance));
            double fMin = Math.max(0, macroTarget.fatMinG         * (1.0 - tolerance));
            double fMax =             macroTarget.fatMaxG         * (1.0 + tolerance);
            double calMin = dailyCalories * (0.85 - tolerance / 2);
            double calMax = dailyCalories * (1.15 + tolerance / 2);

            Expression protExpr = model.addExpression("Protein").lower(pMin).upper(pMax);
            Expression carbsExpr = model.addExpression("Carbs").lower(cMin).upper(9999.0);
            Expression fatExpr   = model.addExpression("Fat").lower(fMin).upper(fMax);
            Expression calExpr   = model.addExpression("Calories").lower(calMin).upper(calMax);

            for (String slotName : slotCandidates.keySet()) {
                for (RecipeCandidateDto c : slotCandidates.get(slotName)) {
                    Variable x = vars.get(slotName).get(String.valueOf(c.getRecipeId()));
                    NutritionModel n = data.getNutrition(c.getRecipeId());
                    double srv = c.getRecommendedServings();

                    protExpr.set(x,  n.getProteinG()    * srv);
                    carbsExpr.set(x, n.getTotalCarbsG() * srv);
                    fatExpr.set(x,   n.getTotalFatG()   * srv);
                    calExpr.set(x,   c.getScaledCalories());
                }
            }

            model.options.time_abort = 2000L;
            Optimisation.Result result = model.maximise();

            if (result.getState().isFeasible()) {
                if (tolerance > 0)
                    log.debug("  ILP solved with tolerance {}%", Math.round(tolerance * 100));

                List<MealSlotDto> finalSlots = buildFinalSlots(
                        slotCandidates, slotBudgets, vars, usageBySlotType, data, day);
                finalSlots.sort(Comparator.comparingInt(s -> slotOrder(s.getMealType())));
                return finalSlots;
            }

            tolerance += 0.05;
            if (tolerance <= 0.60)
                log.debug("  ILP infeasible at {}%, retrying with {}%...",
                        Math.round((tolerance - 0.05) * 100), Math.round(tolerance * 100));
        }

        log.error("ILP FAILED for day {} — could not find feasible solution at 60% tolerance", day);
        return emptyFallback(slotRatios, dailyCalories, "ILP_FAILED");
    }

    // -----------------------------------------------------------------------
    // Candidate generation per slot
    // -----------------------------------------------------------------------

    private List<RecipeCandidateDto> getTopCandidatesForSlot(
            String slotName, double slotBudget, MacroTarget macroTarget,
            UserProfileModel user, RecipeDataContext data,
            ClassificationContext classification,
            Set<Integer> usedToday,
            Map<Integer, Map<String, List<Integer>>> usageBySlotType,
            int currentDay) {

        String effectiveSlot = "snack_2".equals(slotName) ? "snack" : slotName;

        RecipeFilterService.FilterResult fRes =
                recipeFilter.filterRecipes(data, classification, user, effectiveSlot);
        List<RecipeModel> filtered = fRes.recipes();

        boolean relaxMealType = false;
        if (filtered.size() < 10) {
            relaxMealType = true;
            fRes     = recipeFilter.filterRecipes(data, classification, user, "ALL");
            filtered = fRes.recipes();
            log.debug("Slot '{}': <10 recipes, relaxing meal type (pool={})", slotName, filtered.size());
        }

        List<RecipeCandidateDto> candidates = new ArrayList<>();

        for (RecipeModel recipe : filtered) {
            NutritionModel nutrition = data.getNutrition(recipe.getId());
            if (nutrition == null) continue;

            double calsPerServing = nutrition.getCalories();
            if (calsPerServing <= 0) continue;

            List<String> relaxations = new ArrayList<>();

            if (relaxMealType) {
                String rmt = recipe.getMealType();
                if (rmt != null && !rmt.toLowerCase().equals(effectiveSlot)) {
                    if (!RecipeFilterService.matchesAdjacentMealType(rmt, effectiveSlot)) continue;
                    relaxations.add("mealTypeRelaxed");
                }
            }

            double scaledServings = Math.round((slotBudget / calsPerServing) * 10.0) / 10.0;
            scaledServings = Math.max(0.5, Math.min(1.5, scaledServings));
            double scaledCalories = calsPerServing * scaledServings;

            if (scaledCalories < slotBudget * 0.60 || scaledCalories > slotBudget * 1.40) continue;
            if (usedToday.contains((int) recipe.getId())) continue;

            int maxRepeats = user.getMaxRecipeRepeatsPerWeek() != null ? user.getMaxRecipeRepeatsPerWeek() : 2;
            Map<String, List<Integer>> slotMap = usageBySlotType.get((int) recipe.getId());
            if (slotMap != null) {
                int totalUsage = slotMap.values().stream().mapToInt(List::size).sum();
                if (totalUsage >= maxRepeats) continue;
            }

            // gap enforcement
            final int MIN_SAME_SLOT_GAP = 5;
            final int MIN_GLOBAL_GAP    = 3;
            boolean gapViolation = false;
            if (slotMap != null) {
                List<Integer> sameSlotDays = slotMap.get(effectiveSlot);
                if (sameSlotDays != null && !sameSlotDays.isEmpty()) {
                    int lastSame = Collections.max(sameSlotDays);
                    if (currentDay - lastSame < MIN_SAME_SLOT_GAP) gapViolation = true;
                }
                int lastAny = slotMap.values().stream().flatMap(List::stream)
                        .max(Integer::compareTo).orElse(-1);
                if (lastAny != -1 && currentDay - lastAny < MIN_GLOBAL_GAP) gapViolation = true;
            }

            if (gapViolation) {
                if (filtered.size() >= 800) continue;
                if (filtered.size() >= 300 && candidates.size() >= 15) continue;
                relaxations.add("gapRelaxed");
            }

            RecipeCandidateDto candidate = recipeScorer.scoreRecipe(
                    recipe, user, slotBudget, scaledServings, scaledCalories,
                    usageBySlotType, currentDay, macroTarget,
                    data, classification, relaxations, effectiveSlot,
                    BASE_PROTEIN_WEIGHT, RecipeScorerService.ScoringMode.FULL, filtered.size());

            candidates.add(candidate);
        }

        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (candidates.size() > TOP_N_CANDIDATES)
            return new ArrayList<>(candidates.subList(0, TOP_N_CANDIDATES));
        return candidates;
    }

    // -----------------------------------------------------------------------
    // Build final slot list from ILP result
    // -----------------------------------------------------------------------

    private List<MealSlotDto> buildFinalSlots(
            Map<String, List<RecipeCandidateDto>> slotCandidates,
            Map<String, Double> slotBudgets,
            Map<String, Map<String, Variable>> vars,
            Map<Integer, Map<String, List<Integer>>> usageBySlotType,
            RecipeDataContext data,
            int day) {

        List<MealSlotDto> finalSlots = new ArrayList<>();

        for (String slotName : slotCandidates.keySet()) {
            List<RecipeCandidateDto> chosen    = new ArrayList<>();
            RecipeCandidateDto       primary   = null;

            for (RecipeCandidateDto c : slotCandidates.get(slotName)) {
                Variable x = vars.get(slotName).get(String.valueOf(c.getRecipeId()));
                if (x != null && x.getValue().doubleValue() > 0.5) {
                    primary = c;
                    chosen.add(c);
                    String effectiveSlot = "snack_2".equals(slotName) ? "snack" : slotName;
                    usageBySlotType
                            .computeIfAbsent((int) c.getRecipeId(), k -> new HashMap<>())
                            .computeIfAbsent(effectiveSlot, k -> new ArrayList<>())
                            .add(day);
                    break;
                }
            }

            if (primary != null) {
                NutritionModel pn = data.getNutrition(primary.getRecipeId());
                if (pn != null) {
                    double pCals    = primary.getScaledCalories();
                    double pProtein = pn.getProteinG() * primary.getRecommendedServings();

                    // add nutritionally-similar alternatives
                    List<RecipeCandidateDto> alts = new ArrayList<>();
                    for (RecipeCandidateDto c : slotCandidates.get(slotName)) {
                        if (c.getRecipeId() == primary.getRecipeId()) continue;
                        NutritionModel cn = data.getNutrition(c.getRecipeId());
                        if (cn == null) continue;
                        double cCals    = c.getScaledCalories();
                        double cProtein = cn.getProteinG() * c.getRecommendedServings();
                        boolean calClose  = Math.abs(cCals - pCals) / Math.max(1, pCals) <= 0.15;
                        boolean protClose = Math.abs(cProtein - pProtein) / Math.max(1, pProtein) <= 0.20;
                        if (calClose && protClose) alts.add(c);
                    }
                    alts.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
                    alts.stream().limit(4).forEach(chosen::add);
                }

                // ensure at least 3 choices for the user
                if (chosen.size() < 3) {
                    Set<Long> alreadyIn = new HashSet<>();
                    chosen.forEach(c -> alreadyIn.add(c.getRecipeId()));
                    for (RecipeCandidateDto c : slotCandidates.get(slotName)) {
                        if (chosen.size() >= 5) break;
                        if (alreadyIn.add(c.getRecipeId())) chosen.add(c);
                    }
                }
            }

            finalSlots.add(new MealSlotDto(
                    slotName,
                    (int) Math.round(slotBudgets.get(slotName)),
                    chosen,
                    false,
                    "Decided by ILP"));
        }

        return finalSlots;
    }

    // -----------------------------------------------------------------------
    // Daily macro summary
    // -----------------------------------------------------------------------

    private EstimatedDailyMacrosDto computeDailyMacros(
            List<MealSlotDto> slots, MacroTarget mt, RecipeDataContext data) {

        double protein = 0, carbs = 0, fat = 0;
        for (MealSlotDto slot : slots) {
            if (!slot.getCandidates().isEmpty()) {
                RecipeCandidateDto top = slot.getCandidates().get(0);
                NutritionModel n = data.getNutrition(top.getRecipeId());
                if (n != null) {
                    double srv = top.getRecommendedServings();
                    protein += n.getProteinG()    * srv;
                    carbs   += n.getTotalCarbsG() * srv;
                    fat     += n.getTotalFatG()   * srv;
                }
            }
        }

        int pR = (int) Math.round(protein);
        int cR = (int) Math.round(carbs);
        int fR = (int) Math.round(fat);

        int pCov = mt.proteinTargetG > 0 ? (int) Math.round(protein / mt.proteinTargetG * 100) : 0;
        String pStatus = pCov > 150 ? "HIGH" : pCov >= 85 ? "OK" : pCov >= 70 ? "LOW" : "CRITICAL";

        String cStatus = MacroRequirementService.calcCarbsStatus(cR, mt.carbsTargetG);
        int cCov = mt.carbsTargetG > 0 ? (int) Math.round(carbs / mt.carbsTargetG * 100) : 0;

        String fStatus = MacroRequirementService.calcFatStatus(fR, mt.fatMinG, mt.fatMaxG);
        int fCov = mt.fatTargetG > 0 ? (int) Math.round(fat / mt.fatTargetG * 100) : 0;

        return new EstimatedDailyMacrosDto(
                pR, cR, fR,
                mt.proteinTargetG, mt.proteinMinG, mt.proteinMaxG,
                mt.fatTargetG, mt.fatMinG, mt.fatMaxG,
                mt.carbsTargetG, mt.carbsAbsoluteMinG, mt.lowCarbWarning,
                pCov, pStatus, "CRITICAL".equals(pStatus),
                cCov, cStatus, fCov, fStatus);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static int slotOrder(String mealType) {
        return switch (mealType) {
            case "breakfast" -> 1; case "lunch" -> 2; case "dinner" -> 3;
            case "snack" -> 4; case "snack_2" -> 5; default -> 99;
        };
    }

    private static List<MealSlotDto> emptyFallback(
            Map<String, Double> slotRatios, double dailyCalories, String reason) {
        List<MealSlotDto> empty = new ArrayList<>();
        slotRatios.forEach((name, ratio) -> empty.add(new MealSlotDto(
                name, (int) Math.round(dailyCalories * ratio),
                List.of(), true, "No candidates — " + reason)));
        return empty;
    }
}
