package com.feex.mealplannersystem.dto.user;

import com.feex.mealplannersystem.common.survey.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceResponse {
    private Long id;
    private Gender gender;
    private Integer age;
    private Integer heightCm;
    private BigDecimal weightKg;
    private BigDecimal targetWeightKg;
    private ActivityLevel activityLevel;
    private Goal goal;
    private GoalIntensity goalIntensity;
    private DietType dietType;
    private List<String> healthConditions;
    private List<String> allergies;
    private List<String> dislikedIngredients;
    private Integer mealsPerDay;
    private CookComplexity cookingComplexity;
    private CookBudget budgetLevel;
    private boolean zigzag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}