package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.repository.entity.mealplan.MealPlanSlotEntity;

public interface MealSwapService {
    MealPlanSlotEntity swapMainSlot(Long slotId, String userEmail);

    MealPlanSlotEntity swapSideSlot(Long slotId, String userEmail);

    MealPlanSlotEntity swapSlotAuto(Long slotId, String userEmail);
}
