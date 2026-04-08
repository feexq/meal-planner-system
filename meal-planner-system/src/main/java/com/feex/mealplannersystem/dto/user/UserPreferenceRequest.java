package com.feex.mealplannersystem.dto.user;

import com.feex.mealplannersystem.common.survey.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceRequest {

    @NotNull
    private Gender gender;

    @NotNull @Min(10) @Max(120)
    private Integer age;

    @NotNull @Min(100) @Max(250)
    private Integer heightCm;

    @NotNull @DecimalMin("30.0") @DecimalMax("300.0")
    private BigDecimal weightKg;

    @DecimalMin("30.0") @DecimalMax("300.0")
    private BigDecimal targetWeightKg;

    @NotNull
    private ActivityLevel activityLevel;

    @NotNull
    private Goal goal;

    @NotNull
    private GoalIntensity goalIntensity;

    @NotNull
    private DietType dietType;

    private List<String> healthConditions = new ArrayList<>();
    private List<String> allergies = new ArrayList<>();
    private List<String> dislikedIngredients = new ArrayList<>();

    @NotNull @Min(1) @Max(7)
    private Integer mealsPerDay;

    @NotNull
    private CookComplexity cookingComplexity;

    @NotNull
    private CookBudget budgetLevel;

    private boolean zigzag = false;
}