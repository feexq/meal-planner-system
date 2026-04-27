package com.feex.mealplannersystem.config.normalizer.dto.request;

import com.feex.mealplannersystem.dto.mealplan.UserProfilePayload;
import com.feex.mealplannersystem.dto.mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.service.impl.AdditionalRecipeServiceImpl;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SwapSideRequest {
    private Long   slotId;
    private String mealType;
    private double calorieBudget;
    private Map<String, Object> mainRecipe;
    private List<AdditionalRecipeDto> additionalPool;
    private UserProfilePayload userProfile;
}
