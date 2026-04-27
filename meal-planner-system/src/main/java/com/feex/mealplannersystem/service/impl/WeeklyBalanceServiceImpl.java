package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.mealplan.DayTargetDto;
import com.feex.mealplannersystem.dto.mealplan.WeeklyBalanceDto;
import com.feex.mealplannersystem.dto.mealplan.response.AdaptedPlanResponse;
import com.feex.mealplannersystem.repository.FoodLogRepository;
import com.feex.mealplannersystem.repository.MealPlanSlotRepository;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.service.WeeklyBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyBalanceServiceImpl implements WeeklyBalanceService {

    private final FoodLogRepository foodLogRepository;
    private final MealPlanSlotRepository slotRepository;

    private static final double MAX_DEFICIT_PCT  = 0.20;
    private static final double MIN_DAILY_KCAL   = 1200.0;

    private static final Map<String, Double> MEAL_TYPE_RATIO = Map.of(
            "BREAKFAST", 0.25,
            "LUNCH",     0.40,
            "DINNER",    0.35
    );

    public void applyMinimumSafeLimits(MealPlanRecordEntity plan, int currentDay) {
        List<MealPlanSlotEntity> futureSlots = plan.getSlots().stream()
                .filter(s -> s.getDayNumber() > currentDay
                        && s.getStatus() == MealPlanSlotEntity.SlotStatus.PLANNED)
                .toList();

        if (futureSlots.isEmpty()) return;

        Map<Integer, List<MealPlanSlotEntity>> byDay = futureSlots.stream()
                .collect(Collectors.groupingBy(MealPlanSlotEntity::getDayNumber));

        for (var entry : byDay.entrySet()) {
            List<MealPlanSlotEntity> daySlots = entry.getValue();

            for (MealPlanSlotEntity slot : daySlots) {
                String mealType = slot.getMealType() != null
                        ? slot.getMealType().toUpperCase()
                        : "LUNCH";

                double ratio = MEAL_TYPE_RATIO.getOrDefault(mealType, 1.0 / daySlots.size());
                double newTarget = Math.round(MIN_DAILY_KCAL * ratio * 10.0) / 10.0;

                slot.setTargetCalories(newTarget);
            }

            slotRepository.saveAll(daySlots);
            log.info("Plan {}: applied min-safe limits for day {} ({} slots → 1200 kcal total)",
                    plan.getId(), entry.getKey(), daySlots.size());
        }
    }

    public AdaptedPlanResponse recalculate(MealPlanRecordEntity plan, int currentDay) {

        double baseDaily  = plan.getDailyCalorieTarget();
        double weeklyTarget = plan.getWeeklyCalorieTarget();

        double consumedFromSlots = plan.getSlots().stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.EATEN
                        && s.getActualCalories() != null)
                .mapToDouble(MealPlanSlotEntity::getActualCalories)
                .sum();

        double consumedExtra = foodLogRepository.sumExtraCaloriesTotal(plan.getId());
        double totalConsumed  = consumedFromSlots + consumedExtra;
        double remainingBudget = weeklyTarget - totalConsumed;

        int daysToAdjust = 7 - currentDay;

        if (daysToAdjust <= 0) {
            return buildResponse(plan, totalConsumed, remainingBudget,
                    0, baseDaily, baseDaily, false,
                    "Week completed or last day. No future targets to adjust.", List.of());
        }

        double missedPastCalories = plan.getSlots().stream()
                .filter(s -> s.getDayNumber() < currentDay
                        && s.getStatus() == MealPlanSlotEntity.SlotStatus.PLANNED)
                .mapToDouble(MealPlanSlotEntity::getTargetCalories)
                .sum();

        double unEatenTodayCalories = plan.getSlots().stream()
                .filter(s -> s.getDayNumber() == currentDay
                        && s.getStatus() == MealPlanSlotEntity.SlotStatus.PLANNED)
                .mapToDouble(MealPlanSlotEntity::getTargetCalories)
                .sum();

        double budgetForTomorrowOnwards = remainingBudget - missedPastCalories - unEatenTodayCalories;

        double plannedForTomorrowOnwards = plan.getSlots().stream()
                .filter(s -> s.getDayNumber() > currentDay
                        && s.getStatus() == MealPlanSlotEntity.SlotStatus.PLANNED)
                .mapToDouble(MealPlanSlotEntity::getTargetCalories)
                .sum();

        double totalDelta = budgetForTomorrowOnwards - plannedForTomorrowOnwards;

        if (totalDelta > 0) {
            totalDelta = 0;
        }

        double deltaPerDay = totalDelta / daysToAdjust;

        Map<Integer, Double> originalDayTargets = plan.getSlots().stream()
                .collect(Collectors.groupingBy(
                        MealPlanSlotEntity::getDayNumber,
                        Collectors.summingDouble(MealPlanSlotEntity::getTargetCalories)
                ));

        // Групуємо майбутні слоти по днях для детального аналізу сайдів/основних страв
        Map<Integer, List<MealPlanSlotEntity>> plannedSlotsByDay = plan.getSlots().stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.PLANNED)
                .collect(Collectors.groupingBy(MealPlanSlotEntity::getDayNumber));

        List<DayTargetDto> updatedTargets = new ArrayList<>();
        boolean overLimitWarning = false;
        double totalUnrecoverable = 0;

        for (int day = currentDay; day <= 7; day++) {
            double dayOriginalTarget = originalDayTargets.getOrDefault(day, baseDaily);
            double adjustedDaily = dayOriginalTarget;
            double dailyDelta = 0.0;
            String action = "NONE";

            if (day > currentDay) {
                adjustedDaily = dayOriginalTarget + deltaPerDay;

                if (adjustedDaily < MIN_DAILY_KCAL) {
                    overLimitWarning = true;
                    totalUnrecoverable += (MIN_DAILY_KCAL - adjustedDaily);
                    adjustedDaily = MIN_DAILY_KCAL;
                }

                adjustedDaily = Math.round(adjustedDaily * 10.0) / 10.0;
                dailyDelta = Math.round((adjustedDaily - dayOriginalTarget) * 10.0) / 10.0;
                double deficit = Math.abs(dailyDelta);

                if (deficit > 0) {
                    action = determineActionBasedOnSlots(deficit, plannedSlotsByDay.getOrDefault(day, List.of()));
                }
            }

            updatedTargets.add(new DayTargetDto(
                    day,
                    Math.round(dayOriginalTarget * 10.0) / 10.0,
                    adjustedDaily,
                    dailyDelta,
                    action));
        }

        String note;
        if (overLimitWarning) {
            note = String.format("Intake significantly exceeded. Set to min safe limit. %.0f kcal surplus cannot be fully compensated.", totalUnrecoverable);
            log.warn("Plan {}: over-limit, unrecoverable surplus = {} kcal", plan.getId(), totalUnrecoverable);
        } else if (deltaPerDay < 0) {
            note = String.format("Adjusted targets by %.0f kcal/day to compensate for overeating. %d remaining days.", deltaPerDay, daysToAdjust);
        } else {
            note = "Targets are on track. No adjustments needed for under-eating.";
        }

        double avgAdjustedDaily = baseDaily + deltaPerDay;

        return buildResponse(plan, totalConsumed, remainingBudget,
                daysToAdjust, baseDaily, Math.round(avgAdjustedDaily * 10.0) / 10.0,
                overLimitWarning, note, updatedTargets);
    }

    private String determineActionBasedOnSlots(double deficit, List<MealPlanSlotEntity> daySlots) {
        List<MealPlanSlotEntity> sideSlots = daySlots.stream()
                .filter(this::isSideSlot)
                .toList();
        List<MealPlanSlotEntity> mainSlots = daySlots.stream()
                .filter(s -> !isSideSlot(s))
                .toList();

        double totalSideCals = sideSlots.stream()
                .mapToDouble(s -> s.getTargetCalories() != null ? s.getTargetCalories() : 0.0)
                .sum();
        double totalMainCals = mainSlots.stream()
                .mapToDouble(s -> s.getTargetCalories() != null ? s.getTargetCalories() : 0.0)
                .sum();

        double maxSafeMainScaleReduction = totalMainCals * 0.15;

        double maxSingleSideCal = sideSlots.stream()
                .mapToDouble(s -> s.getTargetCalories() != null ? s.getTargetCalories() : 0.0)
                .max().orElse(0.0);

        double absoluteMaxSavings = totalSideCals + (totalMainCals * 0.30);

        if (deficit <= maxSafeMainScaleReduction) {
            return "SCALE_PORTIONS";
        }
        else if (sideSlots.size() > 1 && maxSingleSideCal >= deficit * 0.85) {
            return "DROP_1_SIDE";
        }
        else if (totalSideCals >= deficit * 0.85) {
            return "DROP_SIDES";
        }
        else if (deficit <= absoluteMaxSavings) {
            return "DROP_SIDES_AND_SCALE";
        }
        else {
            return "REQUIRE_SWAP";
        }
    }

    private boolean isSideSlot(MealPlanSlotEntity slot) {
        return slot.getSlotRole() != null && slot.getSlotRole().equalsIgnoreCase("side");
    }

    public WeeklyBalanceDto buildBalance(MealPlanRecordEntity plan, int currentDay) {
        return recalculate(plan, currentDay).getWeeklyBalance();
    }

    private AdaptedPlanResponse buildResponse(
            MealPlanRecordEntity plan,
            double totalConsumed, double remainingBudget,
            int remainingDays, double baseDaily, double avgAdjustedDaily,
            boolean overLimit, String note,
            List<DayTargetDto> targets) {

        WeeklyBalanceDto balance = WeeklyBalanceDto.builder()
                .weeklyCalorieTarget(plan.getWeeklyCalorieTarget())
                .totalConsumed(Math.round(totalConsumed * 10.0) / 10.0)
                .remainingBudget(Math.round(remainingBudget * 10.0) / 10.0)
                .remainingDays(remainingDays)
                .adjustedDailyTarget(avgAdjustedDaily)
                .overLimitWarning(overLimit)
                .adjustmentNote(note)
                .build();

        return new AdaptedPlanResponse(plan.getId(), balance, targets);
    }
}