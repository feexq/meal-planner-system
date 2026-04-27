package com.feex.mealplannersystem.dto.profile;

import com.feex.mealplannersystem.common.user.StreakType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeStreakTypeRequest {
    @NotNull
    private StreakType streakType;
}
