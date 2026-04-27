package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.mealplan.FinalizedMealPlanDto;
import com.feex.mealplannersystem.mealplan.dto.finalize.FinalizedDayDto;
import com.feex.mealplannersystem.mealplan.dto.finalize.FinalizedSlotDto;
import com.feex.mealplannersystem.mealplan.dto.plan.WeeklyMealPlanDto;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanRecordEntity;
import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;
import com.feex.mealplannersystem.repository.MealPlanRecordRepository;
import com.feex.mealplannersystem.repository.UserPreferenceRepository;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.MealPlanPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanPersistenceServiceImpl implements MealPlanPersistenceService {

    private final MealPlanRecordRepository planRepository;

    @Transactional
    public MealPlanRecordEntity saveFinalizedPlan(
            UserEntity user,
            FinalizedMealPlanDto finalizedPlan,
            WeeklyMealPlanDto weeklyPlan) {

        planRepository.findActiveByUserId(user.getId()).ifPresent(old -> {
            old.setStatus(MealPlanRecordEntity.MealPlanStatus.COMPLETED);
            planRepository.save(old);
            log.info("Deactivated old plan {} for user {}", old.getId(), user.getId());
        });

        LocalDate weekStart = LocalDate.now();

        MealPlanRecordEntity record = MealPlanRecordEntity.builder()
                .user(user)
                .weekStartDate(weekStart)
                .dailyCalorieTarget(weeklyPlan.getDailyCalorieTarget())
                .weeklyCalorieTarget(weeklyPlan.getWeeklyCalorieTarget())
                .status(MealPlanRecordEntity.MealPlanStatus.ACTIVE)
                .build();

        Set<MealPlanSlotEntity> slots = new HashSet<>();

        for (FinalizedDayDto day : finalizedPlan.getDays()) {
            for (FinalizedSlotDto slot : day.getSlots()) {

                slots.add(MealPlanSlotEntity.builder()
                        .plan(record)
                        .dayNumber(day.getDay())
                        .mealType(slot.getMealType())
                        .slotRole("main")
                        .recipeId(slot.getMain().getRecipeId())
                        .recipeName(slot.getMain().getName())
                        .targetCalories(slot.getMain().getEstimatedCalories())
                        .recommendedServings(slot.getMain().getServings())
                        .portionNote(slot.getMain().getPortionNote())
                        .proteinG(slot.getMain().getProteinG())
                        .carbsG(slot.getMain().getCarbsG())
                        .fatG(slot.getMain().getFatG())
                        .status(MealPlanSlotEntity.SlotStatus.PLANNED)
                        .build());

                if (slot.getSide() != null) {
                    slots.add(MealPlanSlotEntity.builder()
                            .plan(record)
                            .dayNumber(day.getDay())
                            .mealType(slot.getMealType())
                            .slotRole("side")
                            .recipeId(slot.getSide().getRecipeId())
                            .recipeName(slot.getSide().getName())
                            .targetCalories(slot.getSide().getEstimatedCalories())
                            .recommendedServings(slot.getSide().getServings())
                            .portionNote(slot.getSide().getPortionNote())
                            .proteinG(slot.getSide().getProteinG())
                            .carbsG(slot.getSide().getCarbsG())
                            .fatG(slot.getSide().getFatG())
                            .status(MealPlanSlotEntity.SlotStatus.PLANNED)
                            .build());
                }
            }
        }

        record.setSlots(slots);
        MealPlanRecordEntity saved = planRepository.save(record);

        log.info("Saved plan {} for user {} ({} slots, week {})",
                saved.getId(), user.getId(), slots.size(), weekStart);

        return saved;
    }
}
