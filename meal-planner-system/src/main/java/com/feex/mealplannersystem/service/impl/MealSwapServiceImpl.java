package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.config.normalizer.FinalizeClient;
import com.feex.mealplannersystem.config.normalizer.dto.ChosenItem;
import com.feex.mealplannersystem.config.normalizer.dto.request.SwapSideRequest;
import com.feex.mealplannersystem.config.normalizer.dto.response.SwapSideResponse;
import com.feex.mealplannersystem.dto.mealplan.DayTargetDto;
import com.feex.mealplannersystem.dto.mealplan.UserProfilePayload;
import com.feex.mealplannersystem.dto.mealplan.response.AdaptedPlanResponse;
import com.feex.mealplannersystem.dto.mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.mealplan.common.ScoringMode;
import com.feex.mealplannersystem.mealplan.dto.scoring.RecipeCandidateDto;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataCache;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter;
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.calculator.MacroRequirementService;
import com.feex.mealplannersystem.mealplan.service.filter.RecipeFilterService;
import com.feex.mealplannersystem.mealplan.service.scorer.RecipeScorerService;
import com.feex.mealplannersystem.mealplan.service.scorer.context.ScoringContext;
import com.feex.mealplannersystem.repository.MealPlanRecordRepository;
import com.feex.mealplannersystem.repository.MealPlanSlotRepository;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity;
import com.feex.mealplannersystem.service.MealSwapService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealSwapServiceImpl implements MealSwapService {

    private final MealPlanRecordRepository planRepository;
    private final MealPlanSlotRepository slotRepository;
    private final RecipeDataCache recipeDataCache;
    private final IngredientClassificationAdapter classificationAdapter;
    private final RecipeFilterService recipeFilter;
    private final RecipeScorerService recipeScorer;
    private final MacroRequirementService macroCalculator;
    private final WeeklyBalanceServiceImpl weeklyBalanceService;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileAdapter userProfileAdapter;
    private final AdditionalRecipeServiceImpl additionalRecipeService;
    private final FinalizeClient nlpClient;
    private final RecipeTranslationServiceImpl translationService;

    @Transactional
    public MealPlanSlotEntity swapMainSlot(Long slotId, String userEmail) {
        SwapContext ctx = buildSwapContext(slotId, userEmail, false);
        return doSwapMain(ctx);
    }

    @Transactional
    public MealPlanSlotEntity swapSideSlot(Long slotId, String userEmail) {
        SwapContext ctx = buildSwapContext(slotId, userEmail, true);
        return doSwapSide(ctx);
    }

    @Transactional
    public MealPlanSlotEntity swapSlotAuto(Long slotId, String userEmail) {
        MealPlanSlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new CustomNotFoundException("Slot", slotId.toString()));

        boolean isSide = isSideSlot(slot);
        log.info("Auto-swap slotId={} detected as {}", slotId, isSide ? "SIDE" : "MAIN");

        SwapContext ctx = buildSwapContext(slotId, userEmail, isSide);
        return isSide ? doSwapSide(ctx) : doSwapMain(ctx);
    }

    private boolean isSideSlot(MealPlanSlotEntity slot) {
        if (slot.getSlotRole() != null) {
            return slot.getSlotRole().equals("side");
        }
        return false;
    }

    private record SwapContext(
            MealPlanSlotEntity slot,
            MealPlanRecordEntity plan,
            UserProfileModel user,
            double newSlotCalorieTarget
    ) {}

    private SwapContext buildSwapContext(Long slotId, String userEmail, boolean isSide) {
        UserPreferenceEntity prefs = userPreferenceRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomNotFoundException("Preferences", userEmail));
        UserProfileModel user = userProfileAdapter.toModel(prefs);

        MealPlanRecordEntity plan = planRepository
                .findActiveByUserId(Long.parseLong(user.getUserId()))
                .orElseThrow(() -> new CustomNotFoundException("User", user.getUserId()));

        MealPlanSlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new CustomNotFoundException("Slot", slotId.toString()));

        long daysBetween = ChronoUnit.DAYS.between(plan.getWeekStartDate(), LocalDate.now());
        int currentDay   = Math.max(1, Math.min(7, (int) daysBetween + 1));

        AdaptedPlanResponse adapted = weeklyBalanceService.recalculate(plan, currentDay);

        DayTargetDto targetForSlotDay = adapted.getUpdatedDayTargets().stream()
                .filter(t -> t.getDayNumber() == slot.getDayNumber())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No target found for day " + slot.getDayNumber()));

        double scaleFactor = targetForSlotDay.getOriginalTarget() > 0
                ? targetForSlotDay.getAdjustedTarget() / targetForSlotDay.getOriginalTarget()
                : 1.0;

        double currentSlotCals = slot.getTargetCalories() != null ? slot.getTargetCalories() : 0.0;

        double minCalsAllowed = isSide ? 5.0 : 100.0;
        double newTarget = Math.max(minCalsAllowed, currentSlotCals * scaleFactor);

        log.info("SwapContext slotId={} mealType={} currentCals={} scaleFactor={} newTarget={}",
                slotId, slot.getMealType(), currentSlotCals, scaleFactor, newTarget);

        return new SwapContext(slot, plan, user, newTarget);
    }

    private MealPlanSlotEntity doSwapMain(SwapContext ctx) {
        MealPlanSlotEntity slot = ctx.slot();
        MealPlanRecordEntity plan = ctx.plan();
        UserProfileModel user = ctx.user();
        double newTarget = ctx.newSlotCalorieTarget();

        var data = recipeDataCache.buildContext();
        var classification = classificationAdapter.buildContext();
        var macroTarget = macroCalculator.calculateMacros(user, plan.getDailyCalorieTarget());

        Map<Long, Long> recipeCountInPlan = plan.getSlots().stream()
                .filter(s -> s.getRecipeId() != null && !s.getId().equals(slot.getId()))
                .collect(Collectors.groupingBy(MealPlanSlotEntity::getRecipeId, Collectors.counting()));

        Map<Long, Long> recipeCountInDay = plan.getSlots().stream()
                .filter(s -> s.getDayNumber() == slot.getDayNumber()
                        && s.getRecipeId() != null && !s.getId().equals(slot.getId()))
                .collect(Collectors.groupingBy(MealPlanSlotEntity::getRecipeId, Collectors.counting()));

        var filterResult = recipeFilter.filterRecipes(data, classification, user, slot.getMealType());
        List<RecipeCandidateDto> candidates = new ArrayList<>();

        for (RecipeModel recipe : filterResult.recipes()) {
            if (Objects.equals(recipe.getId(), slot.getRecipeId())) continue;
            if (recipeCountInPlan.getOrDefault(recipe.getId(), 0L) >= 2) continue;
            if (recipeCountInDay.getOrDefault(recipe.getId(), 0L) >= 1) continue;

            NutritionModel nutrition = data.getNutrition(recipe.getId());
            if (nutrition == null) continue;

            double calsPerServing  = nutrition.getCalories();
            double scaledServings  = Math.max(0.5, Math.min(1.5, newTarget / calsPerServing));
            double scaledCalories  = calsPerServing * scaledServings;

            if (scaledCalories < newTarget * 0.80 || scaledCalories > newTarget * 1.20) continue;

            ScoringContext scoringContext = ScoringContext.builder()
                    .slotBudget(newTarget)
                    .slotType(slot.getMealType())
                    .currentDay(slot.getDayNumber())
                    .macroTarget(macroTarget)
                    .usageBySlotType(new HashMap<>())
                    .data(data)
                    .classification(classification)
                    .proteinWeight(15)
                    .mode(ScoringMode.FULL)
                    .poolSize(filterResult.recipes().size())
                    .build();

            candidates.add(recipeScorer.scoreRecipe(
                    recipe,
                    user,
                    scoringContext,
                    scaledServings,
                    scaledCalories,
                    new ArrayList<>()
            ));
        }

        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (candidates.isEmpty()) {
            throw new RuntimeException(
                    "No alternative main dish found within " + Math.round(newTarget) + " kcal");
        }

        int bound = Math.min(candidates.size(), 24);
        RecipeCandidateDto best = candidates.get(new Random().nextInt(bound));

        slot.setRecipeId(best.getRecipeId());
        slot.setRecipeName(best.getRecipeName());
        slot.setTargetCalories(best.getScaledCalories());
        slot.setRecommendedServings(best.getRecommendedServings());

        NutritionModel bestNutrition = data.getNutrition(best.getRecipeId());
        if (bestNutrition != null) {
            slot.setProteinG(bestNutrition.getProteinG() * best.getRecommendedServings());
            slot.setCarbsG(bestNutrition.getTotalCarbsG() * best.getRecommendedServings());
            slot.setFatG(bestNutrition.getTotalFatG() * best.getRecommendedServings());
        }

        System.out.println(best.getRecipeName());

        log.info("Swapped MAIN slotId={} → recipeId={} ({} kcal)",
                slot.getId(), best.getRecipeId(), best.getScaledCalories());

        translationService.getUkrainianNames(Set.of(slot.getRecipeId()))
                .entrySet().stream().findFirst()
                .ifPresent(e -> slot.setRecipeName(e.getValue()));

        return slotRepository.save(slot);
    }

    private MealPlanSlotEntity doSwapSide(SwapContext ctx) {
        MealPlanSlotEntity slot   = ctx.slot();
        UserProfileModel user     = ctx.user();
        MealPlanRecordEntity plan = ctx.plan();
        double budget             = ctx.newSlotCalorieTarget();

        var data = recipeDataCache.buildContext();
        var classification = classificationAdapter.buildContext();
        List<AdditionalRecipeDto> additionalPool =
                additionalRecipeService.getAdditionalRecipesByTarget(user, data, classification, budget);

        MealPlanSlotEntity mainSlot = findMainSlotForDay(plan, slot.getDayNumber(), slot.getId());
        String mainName = mainSlot != null ? mainSlot.getRecipeName() : "unknown";
        double mainCals = mainSlot != null && mainSlot.getTargetCalories() != null
                ? mainSlot.getTargetCalories() : 0.0;

        Map<String, Object> mainRecipeInfo = Map.of(
                "recipeId", mainSlot != null ? mainSlot.getRecipeId() : -1,
                "name",     mainName,
                "calories", mainCals
        );

        for (AdditionalRecipeDto pool : additionalPool) {
            System.out.println(pool.name() + " - " +pool.calories());
        }

        SwapSideRequest nlpRequest = SwapSideRequest.builder()
                .slotId(slot.getId())
                .mealType(slot.getMealType())
                .calorieBudget(budget)
                .mainRecipe(mainRecipeInfo)
                .additionalPool(additionalPool)
                .userProfile(buildUserProfilePayload(user))
                .build();

        SwapSideResponse nlpResponse = nlpClient.swapSide(nlpRequest);

        log.warn("NLP response: {}", nlpResponse);

        if (nlpResponse == null || nlpResponse.getChosen() == null) {
            throw new RuntimeException(
                    "No suitable side dish found within " + Math.round(budget) + " kcal");
        }

        ChosenItem chosen = nlpResponse.getChosen();
        slot.setRecipeId(chosen.getRecipeId());
        slot.setRecipeName(chosen.getName());
        slot.setTargetCalories(chosen.getCalories());
        slot.setRecommendedServings(1.0);
        slot.setProteinG(chosen.getProteinG());
        slot.setCarbsG(chosen.getCarbsG());
        slot.setFatG(chosen.getFatG());

        log.info("Swapped SIDE slotId={} → recipeId={} '{}' ({} kcal)",
                slot.getId(), chosen.getRecipeId(), chosen.getName(), chosen.getCalories());

        translationService.getUkrainianNames(Set.of(slot.getRecipeId()))
                .entrySet().stream().findFirst()
                .ifPresent(e -> slot.setRecipeName(e.getValue()));

        return slotRepository.save(slot);
    }

    private MealPlanSlotEntity findMainSlotForDay(
            MealPlanRecordEntity plan, int dayNumber, Long excludeSlotId) {
        return plan.getSlots().stream()
                .filter(s -> s.getDayNumber() == dayNumber
                        && !s.getId().equals(excludeSlotId)
                        && !isSideSlot(s))
                .findFirst()
                .orElse(null);
    }

    private UserProfilePayload buildUserProfilePayload(UserProfileModel model) {
        return UserProfilePayload.builder()
                .gender(model.getGender())
                .age(model.getAge())
                .weightKg(model.getWeightKg())
                .heightCm(model.getHeightCm())
                .activityLevel(model.getActivityLevel())
                .goal(model.getGoal())
                .dietType(model.getDietType())
                .healthConditions(model.getHealthConditions())
                .allergies(model.getAllergies())
                .dislikedIngredients(model.getDislikedIngredients())
                .mealsPerDay(model.getMealsPerDay())
                .build();
    }
}