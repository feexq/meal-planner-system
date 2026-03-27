package service;

import model.*;
import model.output.RecipeCandidate;
import model.output.ScoreBreakdown;

import java.util.*;

public class RecipeScorer {

    public enum ScoringMode {
        FULL, ILP_MODE
    }

    public static RecipeCandidate scoreRecipe(Recipe recipe, UserProfile user,
            double slotBudget, double scaledServings,
            double scaledCalories,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay,
            MacroRequirementCalculator.MacroTarget macroTarget,
            DataLoader dataLoader,
            List<String> relaxations,
            String slotType,
            int proteinWeight) {
        return scoreRecipe(recipe, user, slotBudget, scaledServings, scaledCalories,
                recipeUsageBySlotType, currentDay, macroTarget, dataLoader,
                relaxations, slotType, proteinWeight, ScoringMode.FULL, 0);
    }

    public static RecipeCandidate scoreRecipe(Recipe recipe, UserProfile user,
            double slotBudget, double scaledServings,
            double scaledCalories,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay,
            MacroRequirementCalculator.MacroTarget macroTarget,
            DataLoader dataLoader,
            List<String> relaxations,
            String slotType,
            int proteinWeight,
            ScoringMode mode) {
        return scoreRecipe(recipe, user, slotBudget, scaledServings, scaledCalories,
                recipeUsageBySlotType, currentDay, macroTarget, dataLoader,
                relaxations, slotType, proteinWeight, mode, 0);
    }

    public static RecipeCandidate scoreRecipe(Recipe recipe, UserProfile user,
            double slotBudget, double scaledServings,
            double scaledCalories,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay,
            MacroRequirementCalculator.MacroTarget macroTarget,
            DataLoader dataLoader,
            List<String> relaxations,
            String slotType,
            int proteinWeight,
            ScoringMode mode,
            int poolSize) {

        NutritionEntry nutritionEntry = dataLoader.getNutrition(recipe.getId());
        double caloriesPerServing = nutritionEntry.getNutrition().getCalories();

        double slotProteinTarget = macroTarget.proteinPerMealG;
        if (slotType != null && slotType.startsWith("snack")) {
            slotProteinTarget *= 0.6;
        }

        int calorieFit = scoreCalorieFit(scaledCalories, slotBudget);

        int complexityMatch = 0;

        int budgetMatch = 0;

        int cookTimeScore = scoreCookTime(recipe.getCookTime(), recipe.getTags());

        int proteinFit = 0;
        if (mode == ScoringMode.FULL) {
            double recipeProteinPerServing = nutritionEntry.getNutrition().getProteinG();
            proteinFit = scoreProteinFit(recipeProteinPerServing, scaledServings,
                    slotProteinTarget, proteinWeight);
        }

        List<String> matchedHealthTags = new ArrayList<>();
        int nutritionQuality = scoreNutritionQuality(recipe.getTags(), user, matchedHealthTags,
                macroTarget.carbsTargetG);

        int variety = scoreVariety(recipe.getId(), user, recipeUsageBySlotType, currentDay, slotType, poolSize);

        int vegProteinBonus = scoreVegProteinBonus(recipe, user);

        int carbsFit = 0;
        if (mode == ScoringMode.FULL) {
            double recipeCarbsScaled = nutritionEntry.getNutrition().getTotalCarbsG() * scaledServings;
            carbsFit = scoreCarbsFit(recipeCarbsScaled, macroTarget.carbsPerMealG,
                    macroTarget.carbsTargetG, recipe.getName(), recipe.getTags());
        }

        int fatFit = 0;
        if (mode == ScoringMode.FULL) {
            double recipeFatScaled = nutritionEntry.getNutrition().getTotalFatG() * scaledServings;
            fatFit = scoreFatFit(recipeFatScaled, macroTarget.fatPerMealG);
        }

        List<String> activeConditions = buildActiveConditions(user);
        Map<String, List<String>> softMatchesByCondition = computeSoftForbiddenMatches(recipe, activeConditions,
                dataLoader);

        int softForbiddenPenalty = calculateSoftForbiddenPenalty(softMatchesByCondition);

        ScoreBreakdown breakdown = new ScoreBreakdown(
                calorieFit, complexityMatch, budgetMatch,
                cookTimeScore, proteinFit, carbsFit, fatFit,
                nutritionQuality, variety, vegProteinBonus,
                softForbiddenPenalty);

        double deviation = ((scaledCalories - slotBudget) / slotBudget) * 100.0;
        deviation = Math.round(deviation * 10.0) / 10.0;

        List<String> userConditions = user.getHealthConditions() != null
                ? user.getHealthConditions()
                : new ArrayList<>();
        List<String> appliedThresholds = RecipeFilter.getAppliedThresholds(userConditions);

        List<String> dietaryNotes = buildDietaryNotes(recipe, user, softMatchesByCondition);

        return new RecipeCandidate(
                recipe.getId(),
                recipe.getName(),
                breakdown.total(),
                caloriesPerServing,
                scaledServings,
                Math.round(scaledCalories * 10.0) / 10.0,
                deviation,
                matchedHealthTags,
                appliedThresholds,
                dietaryNotes,
                relaxations,
                breakdown,
                recipe.getParsedIngredients());
    }

    private static int scoreCalorieFit(double scaledCalories, double budget) {
        if (budget <= 0)
            return 0;
        double deviation = Math.abs(scaledCalories - budget) / budget;
        if (deviation >= 0.30)
            return 0;
        return (int) Math.round(35.0 * (1.0 - deviation / 0.30));
    }

    private static int scoreComplexity(String recipeComplexity, String userComplexity) {
        if ("ANY".equalsIgnoreCase(userComplexity))
            return 12;
        int recipeLvl = RecipeFilter.complexityLevel(recipeComplexity);
        int userLvl = RecipeFilter.complexityLevel(userComplexity);
        if (recipeLvl <= userLvl)
            return 12;
        return (recipeLvl - userLvl == 1) ? 6 : 0;
    }

    private static int scoreBudget(String recipeBudget, String userBudget) {
        int recipeLvl = RecipeFilter.budgetLevel(recipeBudget);
        int userLvl = RecipeFilter.budgetLevel(userBudget);
        if (recipeLvl <= userLvl)
            return 8;
        return (recipeLvl - userLvl == 1) ? 4 : 0;
    }

    private static int scoreCookTime(String cookTime, List<String> tags) {
        if (cookTime == null)
            return 0;
        switch (cookTime.toLowerCase().trim()) {
            case "15 min":
                return 10;
            case "30 min":
                return 8;
            case "60 min":
                return 6;
            case "4 hours": {
                if (tags != null) {
                    for (String tag : tags) {
                        String t = tag.toLowerCase();
                        if ("slow-cooker".equals(t) || "crock-pot".equals(t))
                            return 5;
                    }
                }
                return 0;
            }
            default:
                return 0;
        }
    }

    private static int scoreProteinFit(double recipeProtein, double servings,
            double slotProteinTarget, int maxScore) {
        if (slotProteinTarget <= 0)
            return Math.round(maxScore * 0.5f);
        double actualProtein = recipeProtein * servings;
        double ratio = actualProtein / slotProteinTarget;
        if (ratio >= 1.00)
            return maxScore;
        if (ratio >= 0.80)
            return Math.round(maxScore * 0.8f);
        if (ratio >= 0.60)
            return Math.round(maxScore * 0.5f);
        if (ratio >= 0.40)
            return Math.round(maxScore * 0.3f);
        return 0;
    }

    private static int scoreNutritionQuality(List<String> recipeTags, UserProfile user,
            List<String> matchedHealthTags, int carbsTargetG) {
        int score = 0;
        Set<String> tags = new HashSet<>();
        for (String t : recipeTags)
            tags.add(t.toLowerCase());

        String goal = user.getGoal() != null ? user.getGoal().toUpperCase() : "";

        if (tags.contains("high-protein")) {
            score += 2;
            matchedHealthTags.add("high-protein");
        }
        if (tags.contains("low-fat") && !tags.contains("very-low-carbs")) {

            score += 1;
            matchedHealthTags.add("low-fat");
        }

        if ("WEIGHT_LOSS".equals(goal) && tags.contains("low-calorie")) {
            score += 2;
            matchedHealthTags.add("low-calorie");
        }
        if ("WEIGHT_GAIN".equals(goal) && tags.contains("high-calorie")) {
            score += 2;
            matchedHealthTags.add("high-calorie");
        }

        List<String> conditions = new ArrayList<>();
        if (user.getHealthConditions() != null) {
            for (String c : user.getHealthConditions())
                conditions.add(c.toLowerCase());
        }

        if (conditions.isEmpty())
            return Math.min(10, score);

        if (conditions.contains("hypertension") && tags.contains("low-sodium")) {
            score += 3;
            matchedHealthTags.add("low-sodium");
        }

        if (conditions.contains("diabetes")) {
            boolean isLowCarbProtocol = carbsTargetG <= 130;
            if (isLowCarbProtocol) {
                if (tags.contains("low-carb")) {
                    score += 3;
                    matchedHealthTags.add("low-carb");
                }
                if (tags.contains("very-low-carbs")) {
                    score += 3;
                    matchedHealthTags.add("very-low-carbs");
                }
            }
            if (tags.contains("high-fiber") && !conditions.contains("ibs")) {
                score += 2;
                matchedHealthTags.add("high-fiber");
            }
        }

        if (conditions.contains("celiac_disease") && tags.contains("gluten-free")) {
            score += 3;
            matchedHealthTags.add("gluten-free");
        }

        if (conditions.contains("high_cholesterol")) {
            if (tags.contains("low-cholesterol")) {
                score += 3;
                matchedHealthTags.add("low-cholesterol");
            }
            if (tags.contains("low-saturated-fat")) {
                score += 3;
                matchedHealthTags.add("low-saturated-fat");
            }
            if (tags.contains("low-fat") && !matchedHealthTags.contains("low-fat")) {
                score += 2;
                matchedHealthTags.add("low-fat");
            }
        }

        if (conditions.contains("lactose_intolerance") && tags.contains("lactose-free")) {
            score += 3;
            matchedHealthTags.add("lactose-free");
        }

        if (conditions.contains("ibs") && tags.contains("low-fodmap")) {
            score += 3;
            matchedHealthTags.add("low-fodmap");
        }

        return Math.min(10, score);
    }

    private static int scoreVariety(int recipeId, UserProfile user,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay, String slotType, int poolSize) {

        Map<String, List<Integer>> slotMap = recipeUsageBySlotType.get(recipeId);
        if (slotMap == null || slotMap.isEmpty())
            return 10;

        int totalUsage = 0;
        int mostRecentDay = -1;
        for (List<Integer> days : slotMap.values()) {
            totalUsage += days.size();
            for (int d : days) {
                if (d > mostRecentDay)
                    mostRecentDay = d;
            }
        }

        int limit = user.getMaxRecipeRepeatsPerWeek() != null
                ? user.getMaxRecipeRepeatsPerWeek()
                : 2;
        if (totalUsage >= limit)
            return -100;

        final int p1, p2, p3;
        if (poolSize >= 800) {
            p1 = -120;
            p2 = -80;
            p3 = -40; 
        } else if (poolSize >= 300) {
            p1 = -60;
            p2 = -40;
            p3 = -20; 
        } else {
            p1 = -30;
            p2 = -20;
            p3 = -10; 
        }

        int sameSlotPenalty = 0;
        List<Integer> sameSlotDays = (slotType != null) ? slotMap.get(slotType) : null;
        if (sameSlotDays != null && !sameSlotDays.isEmpty()) {
            int lastSameSlotDay = Collections.max(sameSlotDays);
            int daysAgoSameSlot = currentDay - lastSameSlotDay;
            if (daysAgoSameSlot == 1) {
                sameSlotPenalty = p1;
            } else if (daysAgoSameSlot <= 3) {
                sameSlotPenalty = p2;
            } else {
                sameSlotPenalty = p3;
            }
        }

        int daysAgo = currentDay - mostRecentDay;
        int baseScore = 0;

        if (daysAgo > 0) { 

            if (poolSize >= 800) {

                if (daysAgo == 1)
                    baseScore = -80;
                else if (daysAgo <= 3)
                    baseScore = -40;
                else if (daysAgo <= 5)
                    baseScore = -10;
                else
                    baseScore = 6;
            } else if (poolSize >= 300) {

                if (daysAgo == 1)
                    baseScore = -40;
                else if (daysAgo <= 3)
                    baseScore = -15;
                else
                    baseScore = 6;
            } else {

                if (daysAgo == 1)
                    baseScore = -15;
                else if (daysAgo <= 3)
                    baseScore = -5;
                else
                    baseScore = 6;
            }
        }

        return baseScore + sameSlotPenalty;
    }

    private static int scoreVariety(int recipeId, UserProfile user,
            Map<Integer, Map<String, List<Integer>>> recipeUsageBySlotType,
            int currentDay, String slotType) {
        return scoreVariety(recipeId, user, recipeUsageBySlotType, currentDay, slotType, 0);
    }

    private static int scoreVegProteinBonus(Recipe recipe, UserProfile user) {
        String dietType = user.getDietType().toUpperCase();
        if (!"VEGETARIAN".equals(dietType) && !"VEGAN".equals(dietType))
            return 0;

        List<String> ingredients = recipe.getParsedIngredients();
        boolean hasEgg = false, hasSoyPrio = false;

        for (String ing : ingredients) {
            String s = ing.toLowerCase();
            if (s.matches(".*\\begg(s)?\\b.*"))
                hasEgg = true;
            if (s.contains("tofu") || s.contains("tempeh")
                    || s.contains("edamame") || s.contains("soy"))
                hasSoyPrio = true;
        }

        if ("VEGETARIAN".equals(dietType) && hasEgg)
            return 5;
        if (hasSoyPrio)
            return 5;

        if ("VEGETARIAN".equals(dietType)) {
            for (String ing : ingredients) {
                String s = ing.toLowerCase();
                if (s.contains("greek yogurt") || s.contains("cottage cheese")
                        || s.contains("ricotta"))
                    return 4;
            }
        }

        int matches = 0;
        for (String ing : ingredients) {
            String s = ing.toLowerCase();
            if (s.contains("lentil") || s.contains("chickpea") || s.contains("bean"))
                matches++;
        }
        if (matches >= 2)
            return 4;
        if (matches == 1)
            return 3;

        for (String ing : ingredients) {
            String s = ing.toLowerCase();
            if (s.contains("quinoa") || s.contains("seitan"))
                return 3;
        }

        return 0;
    }

    static int scoreCarbsFit(double recipeCarbsScaled, int carbsPerMealG,
            int dailyCarbsTargetG, String recipeName, List<String> recipeTags) {

        if (dailyCarbsTargetG > 150) {
            String nameLower = recipeName != null ? recipeName.toLowerCase() : "";
            boolean isExplicitLowCarb = nameLower.contains("low carb")
                    || nameLower.contains("low-carb")
                    || nameLower.contains("keto")
                    || nameLower.contains("zero carb");

            if (!isExplicitLowCarb && recipeTags != null) {
                for (String tag : recipeTags) {
                    if ("very-low-carbs".equals(tag.toLowerCase())) {
                        isExplicitLowCarb = true;
                        break;
                    }
                }
            }

            if (isExplicitLowCarb && recipeCarbsScaled < 10.0)
                return 0;
        }

        if (carbsPerMealG <= 0)
            return 5;

        double ratio = recipeCarbsScaled / (double) carbsPerMealG;
        if (ratio >= 0.85)
            return 10;
        else if (ratio >= 0.60)
            return 7;
        else if (ratio >= 0.35)
            return 4;
        else if (ratio >= 0.15)
            return 2;
        else
            return 0;
    }

    static int scoreFatFit(double recipeFatScaled, int fatPerMealG) {
        if (fatPerMealG <= 0)
            return 3;
        double ratio = recipeFatScaled / (double) fatPerMealG;
        if (ratio >= 0.50 && ratio <= 1.50)
            return 5;
        else if (ratio >= 0.30 && ratio <= 1.75)
            return 3;
        else if (ratio > 1.75)
            return 0;
        else
            return 1;
    }

    private static int scoreComponentPenalty(Recipe recipe, String slotType) {
        String nameLower = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
        List<String> ings = recipe.getParsedIngredients();
        int ingCount = ings != null ? ings.size() : 0;

        boolean isComponentByName = false;
        for (String kw : COMPONENT_NAME_KEYWORDS) {

            boolean matched = kw.contains(" ")
                    ? nameLower.contains(kw)
                    : RecipeFilter.containsWordBoundary(nameLower, kw);
            if (matched) {
                isComponentByName = true;
                break;
            }
        }

        boolean isTooFewIngredients = ingCount > 0 && ingCount < 4;

        if (!isComponentByName && !isTooFewIngredients)
            return 0;

        if (hasProteinSource(ings))
            return 0;

        boolean isSnackSlot = slotType != null && slotType.startsWith("snack");
        return isSnackSlot ? -15 : -30;
    }

    private static final List<String> PROTEIN_SOURCE_KEYWORDS = Arrays.asList(
            "chicken", "beef", "pork", "turkey", "lamb", "fish", "salmon",
            "tuna", "shrimp", "prawn", "egg", "eggs", "tofu", "tempeh",
            "lentil", "chickpea", "bean", "beans", "cheese", "cottage cheese",
            "greek yogurt", "ricotta", "ham", "bacon", "sausage", "cod",
            "tilapia", "halibut", "crab", "lobster", "clam", "mussel",
            "seitan", "edamame", "quinoa", "ground beef", "ground turkey",
            "ground chicken", "duck", "venison", "bison");

    private static boolean hasProteinSource(List<String> ings) {
        if (ings == null)
            return false;
        for (String ing : ings) {
            String il = ing.toLowerCase();
            for (String kw : PROTEIN_SOURCE_KEYWORDS) {
                if (il.contains(kw))
                    return true;
            }
        }
        return false;
    }

    private static final List<String> COMPONENT_NAME_KEYWORDS = Arrays.asList(

            "sauce", "glaze", "topping", "frosting", "icing",
            "dressing", "marinade", "vinaigrette",

            "pesto", "guacamole", "hummus", "tapenade",

            "dough", "pie crust", "pizza crust", "pasta dough",
            "tortilla dough", "bread dough",

            "simple syrup", "fruit gel", "gel",

            "meringue topping", "whipped cream", "candied");

    private static int calculateSoftForbiddenPenalty(
            Map<String, List<String>> softMatchesByCondition) {
        if (softMatchesByCondition.isEmpty())
            return 0;

        int totalPenalty = 0;
        Set<String> alreadyPenalized = new HashSet<>();

        for (Map.Entry<String, List<String>> entry : softMatchesByCondition.entrySet()) {
            String cond = entry.getKey();
            List<String> softMatches = entry.getValue();
            RecipeFilter.ConditionSeverity severity = RecipeFilter.getConditionSeverity(cond);

            int penaltyPerMatch;
            switch (severity) {
                case CRITICAL:
                    penaltyPerMatch = 20;
                    break;
                case HIGH:
                    penaltyPerMatch = 15;
                    break;
                case MODERATE:
                    penaltyPerMatch = 10;
                    break;
                case MILD:
                default:
                    penaltyPerMatch = 5;
                    break;
            }

            for (String match : softMatches) {

                if (alreadyPenalized.add(cond + ":" + match.toLowerCase())) {
                    totalPenalty += penaltyPerMatch;
                }
            }
        }
        return totalPenalty;
    }

    private static List<String> buildActiveConditions(UserProfile user) {
        List<String> conditions = new ArrayList<>();
        if (user.getHealthConditions() != null) {
            for (String c : user.getHealthConditions())
                conditions.add(c.toLowerCase());
        }
        if (user.getDietType() != null && !"omnivore".equalsIgnoreCase(user.getDietType())) {
            conditions.add(user.getDietType().toLowerCase());
        }
        return conditions;
    }

    private static Map<String, List<String>> computeSoftForbiddenMatches(
            Recipe recipe, List<String> conditions, DataLoader dataLoader) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (conditions.isEmpty())
            return result;

        ClassifiedIngredientsService service = dataLoader.getClassifiedIngredientsService();
        if (service == null)
            return result;

        List<String> parsedIngredients = recipe.getParsedIngredients();
        for (String cond : conditions) {
            List<String> matches = service.getSoftForbiddenMatches(parsedIngredients, cond);
            if (!matches.isEmpty()) {
                result.put(cond, matches);
            }
        }
        return result;
    }

    private static List<String> buildDietaryNotes(Recipe recipe, UserProfile user,
            Map<String, List<String>> softMatchesByCondition) {
        List<String> dietaryNotes = new ArrayList<>();

        List<String> conditions = new ArrayList<>();
        if (user.getHealthConditions() != null) {
            for (String c : user.getHealthConditions())
                conditions.add(c.toLowerCase());
        }
        if (user.getDietType() != null && !"omnivore".equalsIgnoreCase(user.getDietType())) {
            conditions.add(user.getDietType().toLowerCase());
        }

        Set<String> processedItems = new HashSet<>();
        for (List<String> softMatches : softMatchesByCondition.values()) {
            for (String match : softMatches) {
                if (processedItems.add(match.toLowerCase())) {
                    dietaryNotes.add("Contains acceptable amount of " + match);
                }
            }
        }

        if (conditions.contains("lactose_intolerance")) {
            for (String ing : recipe.getParsedIngredients()) {
                String s = ing.toLowerCase();
                if (s.contains("milk") || s.contains("cheese") || s.contains("cream")
                        || s.contains("butter") || s.contains("yogurt") || s.contains("curd")
                        || s.contains("dairy") || s.contains("whey") || s.contains("kefir")) {
                    dietaryNotes.add("Use lactose-free dairy alternatives (milk, cheese, etc.)");
                    break;
                }
            }
        }

        if (conditions.contains("celiac_disease")) {
            for (String ing : recipe.getParsedIngredients()) {
                String s = ing.toLowerCase();
                if (s.contains("soy sauce") && !s.contains("gluten-free")) {
                    dietaryNotes.add("Use gluten-free soy sauce or tamari");
                    break;
                }
            }
        }

        return dietaryNotes;
    }
}