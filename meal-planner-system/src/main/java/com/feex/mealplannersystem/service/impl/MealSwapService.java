package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.mealplan.AdaptiveDtos;
import com.feex.mealplannersystem.mealplan.dto.RecipeCandidateDto;
import com.feex.mealplannersystem.mealplan.mapper.IngredientClassificationAdapter;
import com.feex.mealplannersystem.mealplan.mapper.RecipeDataAdapter;
import com.feex.mealplannersystem.mealplan.mapper.UserProfileAdapter; // <--- ДОДАНО
import com.feex.mealplannersystem.mealplan.model.NutritionModel;
import com.feex.mealplannersystem.mealplan.model.RecipeModel;
import com.feex.mealplannersystem.mealplan.model.UserProfileModel;
import com.feex.mealplannersystem.mealplan.service.calculator.MacroRequirementService;
import com.feex.mealplannersystem.mealplan.service.filter.RecipeFilterService;
import com.feex.mealplannersystem.mealplan.service.scorer.RecipeScorerService;
import com.feex.mealplannersystem.repository.MealPlanRecordRepository;
import com.feex.mealplannersystem.repository.MealPlanSlotRepository;
import com.feex.mealplannersystem.repository.UserPreferenceRepository; // <--- ДОДАНО
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.entity.preference.UserPreferenceEntity; // <--- ДОДАНО
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.impl.WeeklyBalanceServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealSwapService {

    private final MealPlanRecordRepository planRepository;
    private final MealPlanSlotRepository slotRepository;
    private final RecipeDataAdapter recipeDataAdapter;
    private final IngredientClassificationAdapter classificationAdapter;
    private final RecipeFilterService recipeFilter;
    private final RecipeScorerService recipeScorer;
    private final MacroRequirementService macroCalculator;
    private final WeeklyBalanceServiceImpl weeklyBalanceService;

    // ДОДАНО ДЛЯ ОТРИМАННЯ ПРОФІЛЮ
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileAdapter userProfileAdapter;

    @Transactional
    public MealPlanSlotEntity swapSlotWithDynamicLimits(Long planId, Long slotId, String userEmail) {

        // 1. Отримуємо UserProfileModel (Твоя існуюча правильна логіка)
        UserPreferenceEntity prefs = userPreferenceRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new CustomNotFoundException("Preferences", userEmail));
        UserProfileModel user = userProfileAdapter.toModel(prefs);

        // 2. Дістаємо план і слот з бази
        MealPlanRecordEntity plan = planRepository.findByIdAndUserId(planId, Long.parseLong(user.getUserId()))
                .orElseThrow(() -> new CustomNotFoundException("Plan", planId.toString()));

        MealPlanSlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new CustomNotFoundException("Slot", slotId.toString()));

        if (!slot.getPlan().getId().equals(planId)) {
            throw new IllegalArgumentException("Цей слот не належить до вказаного плану");
        }

        // 3. Рахуємо поточний день
        long daysBetween = ChronoUnit.DAYS.between(plan.getWeekStartDate(), LocalDate.now());
        int currentDay = Math.max(1, Math.min(7, (int) daysBetween + 1));

        // 4. Звертаємося до балансира, щоб він вирахував, чи є переїдання/штрафи
        AdaptiveDtos.AdaptedPlanResponse adaptedPlan = weeklyBalanceService.recalculate(plan, currentDay);

        AdaptiveDtos.DayTargetDto targetForSlotDay = adaptedPlan.getUpdatedDayTargets().stream()
                .filter(t -> t.getDayNumber() == slot.getDayNumber())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Не вдалося знайти ціль для дня слота"));

        // 5. ДИНАМІЧНА МАТЕМАТИКА
        double ratio = 1.0;
        if (targetForSlotDay.getOriginalTarget() > 0) {
            ratio = targetForSlotDay.getAdjustedTarget() / targetForSlotDay.getOriginalTarget();
        }

        double newSlotCalorieTarget = slot.getTargetCalories() * ratio;
        log.info("Original slot calories: {}, New scaled target: {}", slot.getTargetCalories(), newSlotCalorieTarget);

        var data = recipeDataAdapter.buildContext();
        var classification = classificationAdapter.buildContext();
        double baseDailyTarget = plan.getDailyCalorieTarget();
        var macroTarget = macroCalculator.calculateMacros(user, baseDailyTarget);

        var filterResult = recipeFilter.filterRecipes(data, classification, user, slot.getMealType());

        List<RecipeCandidateDto> candidates = new ArrayList<>();

        for (RecipeModel recipe : filterResult.recipes()) {
            if (recipe.getId() == slot.getRecipeId()) continue;

            NutritionModel nutrition = data.getNutrition(recipe.getId());
            if (nutrition == null) continue;

            double calsPerServing = nutrition.getCalories();
            double scaledServings = Math.max(0.5, Math.min(1.5, newSlotCalorieTarget / calsPerServing));
            double scaledCalories = calsPerServing * scaledServings;

            if (scaledCalories < newSlotCalorieTarget * 0.85 || scaledCalories > newSlotCalorieTarget * 1.15) {
                continue;
            }

            RecipeCandidateDto candidate = recipeScorer.scoreRecipe(
                    recipe, user, newSlotCalorieTarget, scaledServings, scaledCalories,
                    new HashMap<>(), slot.getDayNumber(), macroTarget,
                    data, classification, new ArrayList<>(), slot.getMealType(), 15);

            candidates.add(candidate);
        }

        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (candidates.isEmpty()) {
            throw new RuntimeException("Не знайдено підходящої легкої страви для нового ліміту: " + Math.round(newSlotCalorieTarget) + " ккал");
        }

        RecipeCandidateDto bestAlternative = candidates.get(0);

        slot.setRecipeId(bestAlternative.getRecipeId());
        slot.setRecipeName(bestAlternative.getRecipeName());
        slot.setTargetCalories(bestAlternative.getScaledCalories());
        slot.setRecommendedServings(bestAlternative.getRecommendedServings());

        log.info("Swapped slot {} to recipe {} ({} kcal)", slot.getId(), bestAlternative.getRecipeId(), bestAlternative.getScaledCalories());
        return slotRepository.save(slot);
    }
}