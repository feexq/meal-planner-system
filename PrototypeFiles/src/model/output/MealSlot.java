package model.output;

import java.util.List;

public class MealSlot {
    private String mealType;
    private int slotCalorieBudget;
    private List<RecipeCandidate> candidates;
    private boolean lowCoverage;
    private String filteringNote;

    public MealSlot(String mealType, int slotCalorieBudget,
            List<RecipeCandidate> candidates, boolean lowCoverage, String filteringNote) {
        this.mealType = mealType;
        this.slotCalorieBudget = slotCalorieBudget;
        this.candidates = candidates;
        this.lowCoverage = lowCoverage;
        this.filteringNote = filteringNote;
    }

    public String getMealType() {
        return mealType;
    }

    public int getSlotCalorieBudget() {
        return slotCalorieBudget;
    }

    public List<RecipeCandidate> getCandidates() {
        return candidates;
    }

    public boolean isLowCoverage() {
        return lowCoverage;
    }

    public String getFilteringNote() {
        return filteringNote;
    }
}
