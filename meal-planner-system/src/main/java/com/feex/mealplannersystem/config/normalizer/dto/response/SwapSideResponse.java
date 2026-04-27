package com.feex.mealplannersystem.config.normalizer.dto.response;

import com.feex.mealplannersystem.config.normalizer.dto.ChosenItem;
import lombok.Getter;

@Getter
public class SwapSideResponse {
    private Long slotId;
    private ChosenItem chosen;
    private String message;
}
