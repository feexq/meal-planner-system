package com.feex.mealplannersystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feex.mealplannersystem.config.normalizer.FinalizeClient;
import com.feex.mealplannersystem.dto.mealplan.AdaptiveDtos;
import com.feex.mealplannersystem.repository.FoodLogRepository;
import com.feex.mealplannersystem.repository.MealPlanRecordRepository;
import com.feex.mealplannersystem.repository.MealPlanSlotRepository;
import com.feex.mealplannersystem.repository.UserProfileRepository;
import com.feex.mealplannersystem.repository.entity.mealplan.FoodLogEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodLogServiceImpl {

    private final MealPlanRecordRepository planRepository;
    private final MealPlanSlotRepository slotRepository;
    private final FoodLogRepository foodLogRepository;
    private final FoodNutritionCacheServiceImpl cacheService;
    private final WeeklyBalanceServiceImpl balanceService;
    private final FinalizeClient nlpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DailyNutritionSummaryService summaryService;
    private final StreakService streakService;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public AdaptiveDtos.LogFoodResponse logFood(Long userId, Long planId, AdaptiveDtos.LogFoodRequest request) {

        MealPlanRecordEntity plan = planRepository
                .findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new CustomNotFoundException("Plan", planId.toString()));

        FinalizeClient.ParseFoodResponse parsed =
                nlpClient.parseFood(request.getFoodText());

        for (FinalizeClient.ParsedFoodItem item : parsed.getItems()) {
            if (!item.isFromCache()) {
                cacheService.put(item.getName(),
                        FoodNutritionCacheServiceImpl.CachedNutrition.builder()
                                .calories(item.getCalories())
                                .proteinG(item.getProteinG())
                                .carbsG(item.getCarbsG())
                                .fatG(item.getFatG())
                                .quantityDescription(item.getQuantityDescription())
                                .build());
            }
        }

        String minConfidence = parsed.getItems().stream()
                .map(FinalizeClient.ParsedFoodItem::getConfidence)
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
                parsed.getTotalCalories(), planId, request.getDayNumber(), minConfidence);

        triggerSummaryAndStreak(plan, request.getDayNumber());

        int currentDay = request.getDayNumber();
        AdaptiveDtos.WeeklyBalanceDto balance = balanceService.buildBalance(plan, currentDay);

        List<AdaptiveDtos.ParsedFoodItem> responsItems = parsed.getItems().stream()
                .map(i -> new AdaptiveDtos.ParsedFoodItem(
                        i.getName(), i.getOriginal(), i.getQuantityDescription(),
                        i.getCalories(), i.getProteinG(), i.getCarbsG(), i.getFatG(),
                        i.getConfidence(), i.isFromCache()))
                .collect(Collectors.toList());

        return new AdaptiveDtos.LogFoodResponse(
                responsItems,
                parsed.getTotalCalories(), parsed.getTotalProteinG(),
                parsed.getTotalCarbsG(), parsed.getTotalFatG(),
                parsed.getParseNote(), balance);
    }

    @Transactional
    public AdaptiveDtos.AdaptedPlanResponse markSlotEaten(Long userId, Long planId, AdaptiveDtos.MarkSlotEatenRequest request) {

        MealPlanRecordEntity plan = planRepository
                .findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new CustomNotFoundException("Plan", planId.toString()));

        MealPlanSlotEntity slot = slotRepository.findById(request.getSlotId())
                .filter(s -> s.getPlan().getId().equals(planId))
                .orElseThrow(() -> new CustomNotFoundException("Slot", request.getSlotId().toString()));

        double actual = request.getActualCalories() != null
                ? request.getActualCalories()
                : slot.getTargetCalories();

        slot.setActualCalories(actual);
        slot.setStatus(MealPlanSlotEntity.SlotStatus.EATEN);
        slot.setEatenAt(java.time.LocalDateTime.now());
        slotRepository.saveAndFlush(slot);

        log.info("Slot {} marked EATEN ({} kcal), plan={}", slot.getId(), actual, planId);

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

            // ТИМЧАСОВИЙ ЛОГ ДЛЯ ПЕРЕВІРКИ
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
    public AdaptiveDtos.PlanStatusDto getPlanStatus(Long userId, Long planId) {

        MealPlanRecordEntity plan = planRepository
                .findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new CustomNotFoundException("Plan", planId.toString()));

        long daysBetween = ChronoUnit.DAYS.between(plan.getWeekStartDate(), LocalDate.now());
        int currentDay = (int) daysBetween + 1;
        currentDay = Math.max(1, Math.min(7, currentDay));

        AdaptiveDtos.AdaptedPlanResponse adapted = balanceService.recalculate(plan, currentDay);
        List<AdaptiveDtos.DayStatusDto> days = buildDayStatuses(plan);

        return AdaptiveDtos.PlanStatusDto.builder()
                .planId(plan.getId())
                .weekStartDate(plan.getWeekStartDate().toString())
                .weeklyBalance(adapted.getWeeklyBalance())
                .updatedTargets(adapted.getUpdatedDayTargets())
                .days(days)
                .build();
    }

    private List<AdaptiveDtos.DayStatusDto> buildDayStatuses(MealPlanRecordEntity plan) {
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

                    List<AdaptiveDtos.SlotStatusDto> slotDtos = daySlots.stream()
                            .map(s -> AdaptiveDtos.SlotStatusDto.builder()
                                    .slotId(s.getId())
                                    .mealType(s.getMealType())
                                    .recipeId(s.getRecipeId())
                                    .recipeName(s.getRecipeName())
                                    .targetCalories(s.getTargetCalories())
                                    .actualCalories(s.getActualCalories())
                                    .status(s.getStatus().name())
                                    .eatenAt(s.getEatenAt())
                                    .build())
                            .collect(Collectors.toList());

                    List<AdaptiveDtos.LoggedFoodDto> extraFood = plan.getFoodLogs().stream()
                            .filter(f -> f.getDayNumber() == day)
                            .map(f -> AdaptiveDtos.LoggedFoodDto.builder()
                                    .logId(f.getId())
                                    .rawInput(f.getRawInput())
                                    .totalCalories(f.getTotalCalories())
                                    .confidence(f.getConfidence())
                                    .loggedAt(f.getLoggedAt())
                                    .build())
                            .collect(Collectors.toList());

                    double targetForDay = daySlots.stream()
                            .mapToDouble(MealPlanSlotEntity::getTargetCalories).sum();

                    return AdaptiveDtos.DayStatusDto.builder()
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