package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.mealplan.AdaptiveDtos;
import com.feex.mealplannersystem.repository.FoodLogRepository;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
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
public class WeeklyBalanceServiceImpl {

    private final FoodLogRepository foodLogRepository;

    private static final double MAX_DEFICIT_PCT  = 0.20;
    private static final double MIN_DAILY_KCAL   = 1200.0;

    public AdaptiveDtos.AdaptedPlanResponse recalculate(MealPlanRecordEntity plan, int currentDay) {

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

        double maxDeficitPerDay = baseDaily * MAX_DEFICIT_PCT;
        List<AdaptiveDtos.DayTargetDto> updatedTargets = new ArrayList<>();
        boolean overLimitWarning = false;
        double totalUnrecoverable = 0;

        for (int day = currentDay; day <= 7; day++) {
            double dayOriginalTarget = originalDayTargets.getOrDefault(day, baseDaily);
            double adjustedDaily = dayOriginalTarget;
            double dailyDelta = 0.0;
            String action = "NONE";

            if (day > currentDay) {
                double minAllowedDaily  = Math.max(MIN_DAILY_KCAL, dayOriginalTarget - maxDeficitPerDay);
                adjustedDaily = dayOriginalTarget + deltaPerDay;

                if (adjustedDaily < minAllowedDaily) {
                    overLimitWarning = true;
                    totalUnrecoverable += (minAllowedDaily - adjustedDaily);
                    adjustedDaily = minAllowedDaily;
                }

                adjustedDaily = Math.round(adjustedDaily * 10.0) / 10.0;
                dailyDelta = Math.round((adjustedDaily - dayOriginalTarget) * 10.0) / 10.0;

                double deficit = Math.abs(dailyDelta);
                if (deficit > 0) {
                    if (deficit <= 150) {
                        action = "SCALE_PORTIONS";
                    } else if (deficit <= 300) {
                        action = "DROP_SIDES";
                    } else {
                        action = "REQUIRE_SWAP";
                    }
                }
            }

            updatedTargets.add(new AdaptiveDtos.DayTargetDto(
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

    public AdaptiveDtos.WeeklyBalanceDto buildBalance(MealPlanRecordEntity plan, int currentDay) {
        return recalculate(plan, currentDay).getWeeklyBalance();
    }

    private AdaptiveDtos.AdaptedPlanResponse buildResponse(
            MealPlanRecordEntity plan,
            double totalConsumed, double remainingBudget,
            int remainingDays, double baseDaily, double avgAdjustedDaily,
            boolean overLimit, String note,
            List<AdaptiveDtos.DayTargetDto> targets) {

        AdaptiveDtos.WeeklyBalanceDto balance = AdaptiveDtos.WeeklyBalanceDto.builder()
                .weeklyCalorieTarget(plan.getWeeklyCalorieTarget())
                .totalConsumed(Math.round(totalConsumed * 10.0) / 10.0)
                .remainingBudget(Math.round(remainingBudget * 10.0) / 10.0)
                .remainingDays(remainingDays)
                .adjustedDailyTarget(avgAdjustedDaily)
                .overLimitWarning(overLimit)
                .adjustmentNote(note)
                .build();

        return new AdaptiveDtos.AdaptedPlanResponse(plan.getId(), balance, targets);
    }
}