package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.profile.WeeklyAveragesDto;
import com.feex.mealplannersystem.repository.DailyNutritionSummaryRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.FoodLogEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.entity.profile.DailyNutritionSummaryEntity;
import com.feex.mealplannersystem.repository.projection.WeeklyAveragesProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyNutritionSummaryService {

    private final DailyNutritionSummaryRepository summaryRepository;

    @Transactional
    public DailyNutritionSummaryEntity recalculate(
            UserEntity user,
            LocalDate date,
            List<FoodLogEntity> foodLogs,
            List<MealPlanSlotEntity> slots,
            Integer calorieTarget
    ) {
        double logsCalories = foodLogs.stream()
                .mapToDouble(f -> f.getTotalCalories() != null ? f.getTotalCalories() : 0.0)
                .sum();
        double logsProtein = foodLogs.stream()
                .mapToDouble(f -> f.getTotalProteinG() != null ? f.getTotalProteinG() : 0.0).sum();
        double logsCarbs = foodLogs.stream()
                .mapToDouble(f -> f.getTotalCarbsG() != null ? f.getTotalCarbsG() : 0.0).sum();
        double logsFat = foodLogs.stream()
                .mapToDouble(f -> f.getTotalFatG() != null ? f.getTotalFatG() : 0.0).sum();

        double slotsCalories = slots.stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.EATEN)
                .mapToDouble(s -> s.getActualCalories() != null ? s.getActualCalories() : 0.0)
                .sum();
        double slotsProtein = slots.stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.EATEN)
                .mapToDouble(s -> s.getProteinG() != null ? s.getProteinG() : 0.0).sum();
        double slotsCarbs = slots.stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.EATEN)
                .mapToDouble(s -> s.getCarbsG() != null ? s.getCarbsG() : 0.0).sum();
        double slotsFat = slots.stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.EATEN)
                .mapToDouble(s -> s.getFatG() != null ? s.getFatG() : 0.0).sum();

        double totalCalories = logsCalories + slotsCalories;

        List<MealPlanSlotEntity> mainSlots = slots.stream()
                .filter(s -> "main".equals(s.getSlotRole()))
                .toList();

        int plannedSlots = mainSlots.size();
        int eatenSlots = (int) mainSlots.stream()
                .filter(s -> s.getStatus() == MealPlanSlotEntity.SlotStatus.EATEN)
                .count();

        int completionRate = plannedSlots > 0
                ? (int) Math.round((double) eatenSlots / plannedSlots * 100)
                : 0;

        DailyNutritionSummaryEntity summary = summaryRepository
                .findByUserIdAndSummaryDate(user.getId(), date)
                .orElseGet(() -> DailyNutritionSummaryEntity.builder()
                        .user(user)
                        .summaryDate(date)
                        .build());

        summary.setTotalCalories(totalCalories);
        summary.setPlannedSlots(plannedSlots);
        summary.setEatenSlots(eatenSlots);
        summary.setCompletionRate(completionRate);
        summary.setCalorieTarget(calorieTarget);
        summary.setTotalProteinG(logsProtein + slotsProtein);
        summary.setTotalCarbsG(logsCarbs + slotsCarbs);
        summary.setTotalFatG(logsFat + slotsFat);

        DailyNutritionSummaryEntity saved = summaryRepository.save(summary);

        log.debug("Daily summary recalculated for user={} date={}: " +
                "calories={}, completion={}%", user.getId(), date, totalCalories, completionRate);

        return saved;
    }

    @Transactional(readOnly = true)
    public DailyNutritionSummaryEntity getOrEmpty(UserEntity user, LocalDate date) {
        return summaryRepository
                .findByUserIdAndSummaryDate(user.getId(), date)
                .orElseGet(() -> DailyNutritionSummaryEntity.builder()
                        .user(user)
                        .summaryDate(date)
                        .build());
    }

    @Transactional(readOnly = true)
    public List<DailyNutritionSummaryEntity> getRange(Long userId, LocalDate from, LocalDate to) {
        return summaryRepository.findByUserIdAndDateRange(userId, from, to);
    }

    @Transactional(readOnly = true)
    public WeeklyAveragesDto getWeeklyAverages(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);

        WeeklyAveragesProjection proj = summaryRepository.getAveragesByUserIdAndDateRange(userId, weekStart, weekEnd);

        if (proj == null || proj.getAvgCalories() == null) {
            return WeeklyAveragesDto.empty();
        }

        int cleanDays = summaryRepository.countCompletedDays(userId, weekStart, weekEnd, 80);

        return WeeklyAveragesDto.builder()
                .avgCalories(proj.getAvgCalories())
                .avgProteinG(proj.getAvgProteinG())
                .avgCarbsG(proj.getAvgCarbsG())
                .avgFatG(proj.getAvgFatG())
                .avgCompletionRate(proj.getAvgCompletionRate())
                .cleanDays(cleanDays)
                .build();
    }

    private double toDouble(Object val) {
        return val instanceof Number n ? n.doubleValue() : 0.0;
    }
}