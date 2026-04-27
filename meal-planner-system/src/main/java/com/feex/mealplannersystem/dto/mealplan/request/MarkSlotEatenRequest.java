package com.feex.mealplannersystem.dto.mealplan.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MarkSlotEatenRequest {
    private Long slotId;
    private Double actualCalories;
}

