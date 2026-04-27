package com.feex.mealplannersystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feex.mealplannersystem.config.normalizer.FinalizeClient;
import com.feex.mealplannersystem.config.normalizer.dto.response.ParseFoodResponse;
import com.feex.mealplannersystem.dto.mealplan.LoggedFoodDto;
import com.feex.mealplannersystem.dto.mealplan.ParsedFoodItem;
import com.feex.mealplannersystem.dto.mealplan.WeeklyBalanceDto;
import com.feex.mealplannersystem.dto.mealplan.nutrition.CachedNutrition;
import com.feex.mealplannersystem.dto.mealplan.request.LogFoodRequest;
import com.feex.mealplannersystem.dto.mealplan.request.MarkSlotEatenRequest;
import com.feex.mealplannersystem.dto.mealplan.response.AdaptedPlanResponse;
import com.feex.mealplannersystem.dto.mealplan.response.LogFoodResponse;
import com.feex.mealplannersystem.dto.mealplan.status.DayStatusDto;
import com.feex.mealplannersystem.dto.mealplan.status.PlanStatusDto;
import com.feex.mealplannersystem.dto.mealplan.status.SlotStatusDto;
import com.feex.mealplannersystem.repository.*;
import com.feex.mealplannersystem.repository.entity.mealplan.FoodLogEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.service.FoodLogService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodLogServiceImpl implements FoodLogService {

    private final MealPlanRecordRepository planRepository;
    private final MealPlanSlotRepository slotRepository;
    private final FoodLogRepository foodLogRepository;
    private final FoodNutritionCacheServiceImpl cacheService;
    private final WeeklyBalanceServiceImpl balanceService;
    private final FinalizeClient nlpClient;
    private final RecipeTranslationServiceImpl translationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DailyNutritionSummaryServiceImpl summaryService;
    private final StreakService streakService;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public LogFoodResponse logFood(Long userId, LogFoodRequest request) {

        MealPlanRecordEntity plan = planRepository
                .findActiveByUserId(userId)
                .orElseThrow(() -> new CustomNotFoundException("User", userId.toString()));

        ParseFoodResponse parsed =
                nlpClient.parseFood(request.getFoodText());

        for (ParsedFoodItem item : parsed.getItems()) {
            if (!item.isFromCache()) {
                cacheService.put(item.getName(),
                        CachedNutrition.builder()
                                .calories(item.getCalories())
                                .proteinG(item.getProteinG())
                                .carbsG(item.getCarbsG())
                                .fatG(item.getFatG())
                                .quantityDescription(item.getQuantityDescription())
                                .build());
            }
        }

        String minConfidence = parsed.getItems().stream()
                .map(ParsedFoodItem::getConfidence)
                .min((a, b) -> confidenceOrder(a) - confidenceOrder(b))
                .orElse("medium");

        FoodLogEntity foodLog = FoodLogEntity.builder()
                .plan(plan)
                .dayNumber(request.getDayNumber())
                .rawInput(request.getFoodText())
                .parsedItemsJson(serializeItems(parsed.getItems()))
                .totalCalories(parsed.getTotalCalories())
                .totalProteinG(parsed.getTotalProteinG())
                .totalCarbsG(parsed.getTotalCarbsG())
                .totalFatG(parsed.getTotalFatG())
                .confidence(minConfidence)
                .build();

        foodLogRepository.save(foodLog);
        log.info("Logged {} kcal for plan={} day={} (confidence={})",
                parsed.getTotalCalories(), plan.getId(), request.getDayNumber(), minConfidence);

        triggerSummaryAndStreak(plan, request.getDayNumber());

        int currentDay = request.getDayNumber();
        WeeklyBalanceDto balance = balanceService.buildBalance(plan, currentDay);

        List<ParsedFoodItem> responsItems = parsed.getItems().stream()
                .map(i -> new ParsedFoodItem(
                        i.getName(), i.getOriginal(), i.getQuantityDescription(),
                        i.getCalories(), i.getProteinG(), i.getCarbsG(), i.getFatG(),
                        i.getConfidence(), i.isFromCache()))
                .collect(Collectors.toList());

        return new LogFoodResponse(
                responsItems,
                parsed.getTotalCalories(), parsed.getTotalProteinG(),
                parsed.getTotalCarbsG(), parsed.getTotalFatG(),
                parsed.getParseNote(), balance);
    }

    @Transactional
    public AdaptedPlanResponse markSlotEaten(Long userId, MarkSlotEatenRequest request) {

        MealPlanRecordEntity plan = planRepository
                .findActiveByUserId(userId)
                .orElseThrow(() -> new CustomNotFoundException("User", userId.toString()));

        MealPlanSlotEntity slot = slotRepository.findById(request.getSlotId())
                .filter(s -> s.getPlan().getId().equals(plan.getId()))
                .orElseThrow(() -> new CustomNotFoundException("Slot", request.getSlotId().toString()));

        double actual = request.getActualCalories() != null
                ? request.getActualCalories()
                : slot.getTargetCalories();

        slot.setActualCalories(actual);
        slot.setStatus(MealPlanSlotEntity.SlotStatus.EATEN);
        slot.setEatenAt(java.time.LocalDateTime.now());
        slotRepository.saveAndFlush(slot);

        log.info("Slot {} marked EATEN ({} kcal), plan={}", slot.getId(), actual, plan.getId());

        triggerSummaryAndStreak(plan, slot.getDayNumber());

        return balanceService.recalculate(plan, slot.getDayNumber());
    }

    private void triggerSummaryAndStreak(MealPlanRecordEntity plan, int dayNumber) {
        try {
            LocalDate dayDate = plan.getWeekStartDate().plusDays(dayNumber - 1L);

            List<FoodLogEntity> foodLogs = foodLogRepository
                    .findByPlanIdAndDayNumber(plan.getId(), dayNumber);

            List<MealPlanSlotEntity> daySlots = slotRepository.findByPlanId(plan.getId()).stream()
                    .filter(s -> s.getDayNumber() == dayNumber)
                    .collect(Collectors.toList());

            DailyNutritionSummaryEntity summary = summaryService.recalculate(
                    plan.getUser(),
                    dayDate,
                    foodLogs,
                    daySlots,
                    plan.getDailyCalorieTarget()
            );

            log.info("SUMMARY CHECK: day={}, calories={}", dayDate, summary.getTotalCalories());

            String timezone = userProfileRepository
                    .findByUserId(plan.getUser().getId())
                    .map(p -> p.getTimezone())
                    .orElse("UTC");

            LocalDate today = LocalDate.now(ZoneId.of(timezone));

            if (!dayDate.isAfter(today)) {
                streakService.evaluate(plan.getUser(), summary, dayDate);
            }

        } catch (Exception e) {
            log.error("Failed to update daily summary/streak for plan={} day={}: {}",
                    plan.getId(), dayNumber, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PlanStatusDto getPlanStatus(Long userId) {

        MealPlanRecordEntity plan = planRepository
                .findActiveByUserId(userId)
                .orElseThrow(() -> new CustomNotFoundException("Plan", userId.toString()));

        Set<MealPlanSlotEntity> slots = plan.getSlots();
        Set<FoodLogEntity> logs = plan.getFoodLogs();

        long daysBetween = ChronoUnit.DAYS.between(plan.getWeekStartDate(), LocalDate.now());

        if (daysBetween >= 7) {
            plan.setStatus(MealPlanRecordEntity.MealPlanStatus.COMPLETED);
            planRepository.save(plan);
            throw new CustomNotFoundException("Active plan", userId.toString());
        }

        int currentDay = (int) daysBetween + 1;
        currentDay = Math.max(1, Math.min(7, currentDay));

        AdaptedPlanResponse adapted = balanceService.recalculate(plan, currentDay);
        List<DayStatusDto> days = buildDayStatuses(plan);

        return PlanStatusDto.builder()
                .planId(plan.getId())
                .weekStartDate(plan.getWeekStartDate().toString())
                .weeklyBalance(adapted.getWeeklyBalance())
                .updatedTargets(adapted.getUpdatedDayTargets())
                .days(days)
                .build();
    }

    private List<DayStatusDto> buildDayStatuses(MealPlanRecordEntity plan) {
        return plan.getSlots().stream()
                .collect(Collectors.groupingBy(MealPlanSlotEntity::getDayNumber))
                .entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(e -> {
                    int day = e.getKey();
                    List<MealPlanSlotEntity> daySlots = e.getValue();

                    double planned = daySlots.stream()
                            .mapToDouble(MealPlanSlotEntity::getTargetCalories).sum();
                    double consumed = daySlots.stream()
                            .filter(s -> s.getActualCalories() != null)
                            .mapToDouble(MealPlanSlotEntity::getActualCalories).sum();
                    double extra = foodLogRepository
                            .sumExtraCaloriesByDay(plan.getId(), day);

                    Set<Long> recipeIds = daySlots.stream()
                            .map(MealPlanSlotEntity::getRecipeId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    Map<Long, RecipeTranslationServiceImpl.RecipeTranslationInfo> info =
                            translationService.getTranslationInfo(recipeIds);

                    List<SlotStatusDto> slotDtos = daySlots.stream()
                            .map(s -> {
                                RecipeTranslationServiceImpl.RecipeTranslationInfo t = info.get(s.getRecipeId());
                                return SlotStatusDto.builder()
                                        .slotId(s.getId())
                                        .mealType(s.getMealType())
                                        .recipeId(s.getRecipeId())
                                        .recipeName(t != null && t.name() != null ? t.name() : s.getRecipeName())
                                        .recipeSlug(t != null ? t.slug() : null)
                                        .targetCalories(s.getTargetCalories())
                                        .actualCalories(s.getActualCalories())
                                        .proteinG(s.getProteinG())
                                        .fatG(s.getFatG())
                                        .carbsG(s.getCarbsG())
                                        .slotRole(s.getSlotRole())
                                        .status(s.getStatus().name())
                                        .eatenAt(s.getEatenAt())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    List<LoggedFoodDto> extraFood = plan.getFoodLogs().stream()
                            .filter(f -> f.getDayNumber() == day)
                            .map(f -> LoggedFoodDto.builder()
                                    .logId(f.getId())
                                    .rawInput(f.getRawInput())
                                    .totalCalories(f.getTotalCalories())
                                    .proteinG(f.getTotalProteinG())
                                    .carbsG(f.getTotalCarbsG())
                                    .fatG(f.getTotalFatG())
                                    .confidence(f.getConfidence())
                                    .loggedAt(f.getLoggedAt())
                                    .build())
                            .collect(Collectors.toList());

                    double targetForDay = daySlots.stream()
                            .mapToDouble(MealPlanSlotEntity::getTargetCalories).sum();

                    return DayStatusDto.builder()
                            .dayNumber(day)
                            .targetCalories(targetForDay)
                            .plannedCalories(planned)
                            .consumedCalories(consumed + extra)
                            .extraCalories(extra)
                            .slots(slotDtos)
                            .extraFood(extraFood)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private int confidenceOrder(String confidence) {
        return switch (confidence) {
            case "high"   -> 2;
            case "medium" -> 1;
            default       -> 0;
        };
    }

    private String serializeItems(List<?> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            return "[]";
        }
    }
}