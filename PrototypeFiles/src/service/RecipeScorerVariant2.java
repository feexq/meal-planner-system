package service;

import model.*;
import model.output.RecipeCandidate;

import java.util.*;

public class RecipeScorerVariant2 {

    public static RecipeCandidate scoreRecipe(Recipe recipe, UserProfile user,
            double slotBudget, double scaledServings,
            double scaledCalories,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay,
            MacroRequirementCalculator.MacroTarget macroTarget,
            DataLoader dataLoader,
            List<String> relaxations) {

        return RecipeScorer.scoreRecipe(
                recipe, user, slotBudget, scaledServings, scaledCalories,
                recipeUsageBySlotType, currentDay, macroTarget, dataLoader,
                relaxations,
                null, 
                15, 
                RecipeScorer.ScoringMode.ILP_MODE);
    }
}