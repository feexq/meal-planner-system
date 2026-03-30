package com.feex.mealplannersystem.domain.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MealSlot {
    private String mealType;
    private int slotCalorieBudget;
    private List<RecipeCandidate> candidates;
    private boolean lowCoverage;
    private String filteringNote;
}
