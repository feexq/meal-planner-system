package com.feex.mealplannersystem.mealplan.service.scorer.context;

import com.feex.mealplannersystem.mealplan.common.ScoringMode;
import com.feex.mealplannersystem.mealplan.mapper.context.ClassificationContext;
import com.feex.mealplannersystem.mealplan.mapper.context.RecipeDataContext;
import com.feex.mealplannersystem.mealplan.service.calculator.MacroTarget;
import com.feex.mealplannersystem.mealplan.service.scorer.RecipeScorerService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ScoringContext {
    private double slotBudget;
    private String slotType;
    private int currentDay;
    private int proteinWeight;
    private int poolSize;
    @Builder.Default
    private ScoringMode mode = ScoringMode.FULL;
    private MacroTarget macroTarget;
    private Map<Integer, Map<String, List<Integer>>> usageBySlotType;
    private RecipeDataContext data;
    private ClassificationContext classification;
}
