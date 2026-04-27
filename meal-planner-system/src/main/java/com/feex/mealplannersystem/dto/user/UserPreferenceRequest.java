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
    @NotNull(message = "gender is required")
    private Gender gender;

    @NotNull(message = "age is required")
    @Min(value = 10, message = "age must be at least 10")
    @Max(value = 120, message = "age must be at most 120")
    private Integer age;

    @NotNull(message = "height is required")
    @Min(value = 100, message = "height must be at least 100 cm")
    @Max(value = 250, message = "height must be at most 250 cm")
    private Integer heightCm;

    @NotNull(message = "weight is required")
    @DecimalMin(value = "30.0", message = "weight must be at least 30.0 kg")
    @DecimalMax(value = "300.0", message = "weight must be at most 300.0 kg")
    private BigDecimal weightKg;

    @DecimalMin(value = "30.0", message = "target weight must be at least 30.0 kg")
    @DecimalMax(value = "300.0", message = "target weight must be at most 300.0 kg")
    private BigDecimal targetWeightKg;

    @NotNull(message = "activity level is required")
    private ActivityLevel activityLevel;

    @NotNull(message = "goal is required")
    private Goal goal;

    @NotNull(message = "goal intensity is required")
    private GoalIntensity goalIntensity;

    @NotNull(message = "diet type is required")
    private DietType dietType;

    private List<String> healthConditions = new ArrayList<>();
    private List<String> allergies = new ArrayList<>();
    private List<String> dislikedIngredients = new ArrayList<>();

    @NotNull(message = "meals per day is required")
    @Min(value = 1, message = "meals per day must be at least 1")
    @Max(value = 5, message = "meals per day must be at most 5")
    private Integer mealsPerDay;

    @NotNull(message = "cooking complexity is required")
    private CookComplexity cookingComplexity;

    @NotNull(message = "budget level is required")
    private CookBudget budgetLevel;

    private boolean zigzag = false;
}
