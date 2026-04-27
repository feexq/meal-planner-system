package com.feex.mealplannersystem.mealplan.dto.finalize;

import com.feex.mealplannersystem.dto.mealplan.UserProfilePayload;
import com.feex.mealplannersystem.dto.mealplan.score.AdditionalRecipeDto;
import com.feex.mealplannersystem.mealplan.dto.plan.DayPlanDto;
import com.feex.mealplannersystem.service.impl.AdditionalRecipeServiceImpl;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class FinalizeRequestDto {
    private String userId;
    private int dailyCalorieTarget;
    private int weeklyCalorieTarget;
    private List<DayPlanDto> days;
    private UserProfilePayload userProfile;
    private List<AdditionalRecipeDto> additionalRecipes;
}
