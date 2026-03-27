package service;

import model.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RecipeFilter {

    public enum ConditionSeverity {
        CRITICAL, HIGH, MODERATE, MILD
    }

    public static ConditionSeverity getConditionSeverity(String condition) {
        switch (condition.toLowerCase()) {

            case "celiac_disease":
            case "nut_allergy":
            case "shellfish_allergy":
            case "fish_allergy":
                return ConditionSeverity.CRITICAL;

            case "diabetes":
            case "kidney_disease":
                return ConditionSeverity.HIGH;

            case "hypertension":
            case "high_cholesterol":
            case "gout":
            case "pancreatitis":
                return ConditionSeverity.MODERATE;

            case "ibs":
            case "gastritis":
            case "gerd":
            case "lactose_intolerance":
            default:
                return ConditionSeverity.MILD;
        }
    }

    public static FilterResult filterRecipes(List<Recipe> allRecipes, UserProfile user,
            DataLoader dataLoader, String slotMealType) {
        Map<String, Integer> eliminationCounts = new LinkedHashMap<>();
        eliminationCounts.put("meal_type_mismatch", 0);
        eliminationCounts.put("semantic_ban", 0);
        eliminationCounts.put("celiac_gluten_ingredient", 0);
        eliminationCounts.put("missing_nutrition", 0);
        eliminationCounts.put("diet_violation_ingredient", 0);

        eliminationCounts.put("diet_violation_classified", 0);
        eliminationCounts.put("health_condition_classified", 0);
        eliminationCounts.put("health_condition_numeric", 0);
        eliminationCounts.put("allergy", 0);
        eliminationCounts.put("disliked_ingredient", 0);
        eliminationCounts.put("cook_time_exclude", 0);
        eliminationCounts.put("side_dish_excluded", 0);

        String userDiet = user.getDietType().toLowerCase();
        List<String> userConditions = user.getHealthConditions().stream()
                .map(String::toLowerCase).collect(Collectors.toList());
        List<String> userAllergies = user.getAllergies().stream()
                .map(String::toLowerCase).collect(Collectors.toList());
        List<String> userDisliked = new ArrayList<>(user.getDislikedIngredients().stream()
                .map(String::toLowerCase).collect(Collectors.toList()));

        if (userConditions.contains("gastritis")) {
            if (!userDisliked.contains("fried_food"))
                userDisliked.add("fried_food");
            if (!userDisliked.contains("spicy"))
                userDisliked.add("spicy");
        }

        if (userConditions.contains("gerd")) {
            if (!userDisliked.contains("spicy"))
                userDisliked.add("spicy");
        }

        String effectiveMealType = slotMealType.equals("snack_2") ? "snack" : slotMealType;

        List<Recipe> result = new ArrayList<>();

        for (Recipe recipe : allRecipes) {

            if (!"all".equalsIgnoreCase(effectiveMealType)) {
                if (!matchesMealType(recipe.getMealType(), effectiveMealType)) {
                    eliminationCounts.merge("meal_type_mismatch", 1, Integer::sum);
                    continue;
                }

            }

            if ("1+ days".equalsIgnoreCase(recipe.getCookTime())) {
                eliminationCounts.merge("cook_time_exclude", 1, Integer::sum);
                continue;
            }

            NutritionEntry nutrition = dataLoader.getNutrition(recipe.getId());
            if (nutrition == null) {
                eliminationCounts.merge("missing_nutrition", 1, Integer::sum);
                continue;
            }

            IngredientLookupService.IngredientLookupResult lookup = dataLoader.getLookupResult(recipe.getId());
            if (lookup != null && !"omnivore".equals(userDiet) && lookup.isNotSuitableFor(userDiet)) {
                eliminationCounts.merge("diet_violation_ingredient", 1, Integer::sum);
                continue;
            }

            if (!"omnivore".equals(userDiet)
                    && hasClassifiedIngredientViolation(recipe, Collections.singletonList(userDiet), dataLoader)) {
                eliminationCounts.merge("diet_violation_classified", 1, Integer::sum);
                continue;
            }

            if (hasClassifiedIngredientViolation(recipe, userConditions, dataLoader)) {
                eliminationCounts.merge("health_condition_classified", 1, Integer::sum);
                continue;
            }

            boolean lookupViolation = false;
            if (lookup != null) {
                for (String condition : userConditions) {
                    if (lookup.hasContraindication(condition)) {
                        lookupViolation = true;
                        break;
                    }
                }
            }
            if (lookupViolation) {
                eliminationCounts.merge("health_condition_classified", 1, Integer::sum);
                continue;
            }

            if (hasHealthConditionNumericViolation(nutrition.getNutrition(), userConditions)) {
                eliminationCounts.merge("health_condition_numeric", 1, Integer::sum);
                continue;
            }

            if (("dinner".equals(effectiveMealType) || "lunch".equals(effectiveMealType))
                    && recipe.getParsedIngredients().size() <= 5
                    && nutrition.getNutrition().getProteinG() < 5.0) {
                eliminationCounts.merge("side_dish_excluded", 1, Integer::sum);
                continue;
            }

            if (hasAllergyViolation(recipe, userAllergies)) {
                eliminationCounts.merge("allergy", 1, Integer::sum);
                continue;
            }

            if (hasDislikedIngredient(recipe, userDisliked)) {
                eliminationCounts.merge("disliked_ingredient", 1, Integer::sum);
                continue;
            }

            result.add(recipe);
        }

        String filteringNote = null;
        if (result.isEmpty()) {
            String topFilter = eliminationCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + " (" + e.getValue() + " recipes)")
                    .orElse("unknown");
            filteringNote = "No recipes passed filtering. Top elimination cause: " + topFilter;
        }

        return new FilterResult(result, filteringNote, eliminationCounts);
    }

    public static List<String> getAppliedThresholds(List<String> userConditions) {
        List<String> thresholds = new ArrayList<>();
        for (String condition : userConditions) {
            switch (condition.toLowerCase()) {
                case "diabetes":
                    thresholds.add("sugars_g \u2264 10.0");
                    thresholds.add("total_carbs_g \u2264 45.0");
                    break;
                case "hypertension":
                    thresholds.add("sodium_mg \u2264 600.0");
                    break;
                case "high_cholesterol":
                    thresholds.add("saturated_fat_g \u2264 7.0");
                    thresholds.add("cholesterol_mg \u2264 100.0");
                    break;
                case "kidney_disease":
                    thresholds.add("sodium_mg \u2264 400.0");
                    thresholds.add("protein_g \u2264 20.0");
                    break;
                case "gout":
                    thresholds.add("protein_g \u2264 25.0");
                    break;
            }
        }
        return thresholds;
    }

    private static boolean hasClassifiedIngredientViolation(Recipe recipe, List<String> conditions,
            DataLoader dataLoader) {
        ClassifiedIngredientsService service = dataLoader.getClassifiedIngredientsService();
        if (service == null || conditions.isEmpty())
            return false;

        for (String condition : conditions) {
            if (service.hasForbiddenIngredient(recipe.getParsedIngredients(), condition)) {
                return true;
            }
        }
        return false;
    }

    public static void debugFilterBreakdown(List<Recipe> allRecipes, UserProfile user,
            DataLoader dataLoader, String slotMealType) {

        String userDiet = user.getDietType().toLowerCase();
        List<String> userConditions = user.getHealthConditions().stream()
                .map(String::toLowerCase).collect(Collectors.toList());

        ClassifiedIngredientsService service = dataLoader.getClassifiedIngredientsService();
        String effectiveMealType = slotMealType.equals("snack_2") ? "snack" : slotMealType;

        int total = allRecipes.size();
        int afterMealType = 0, afterNutrition = 0, afterDietLookup = 0,
                afterDietClassified = 0, afterCondClassified = 0,
                afterCondLookup = 0, afterNumeric = 0;

        Map<String, Integer> condClassifiedHits = new LinkedHashMap<>();
        for (String c : userConditions)
            condClassifiedHits.put(c, 0);
        int dietClassifiedHits = 0;

        for (Recipe recipe : allRecipes) {
            if (!"all".equalsIgnoreCase(effectiveMealType)
                    && !matchesMealType(recipe.getMealType(), effectiveMealType))
                continue;
            afterMealType++;

            NutritionEntry nutrition = dataLoader.getNutrition(recipe.getId());
            if (nutrition == null)
                continue;
            afterNutrition++;

            IngredientLookupService.IngredientLookupResult lookup = dataLoader.getLookupResult(recipe.getId());
            if (lookup != null && !"omnivore".equals(userDiet) && lookup.isNotSuitableFor(userDiet))
                continue;
            afterDietLookup++;

            boolean dietViol = false;
            if (!"omnivore".equals(userDiet) && service != null) {
                if (service.hasForbiddenIngredient(recipe.getParsedIngredients(), userDiet)) {
                    dietViol = true;
                    dietClassifiedHits++;
                }
            }
            if (dietViol)
                continue;
            afterDietClassified++;

            boolean condViol = false;
            if (service != null) {
                for (String cond : userConditions) {
                    if (service.hasForbiddenIngredient(recipe.getParsedIngredients(), cond)) {
                        condClassifiedHits.merge(cond, 1, Integer::sum);
                        condViol = true;
                        break;
                    }
                }
            }
            if (condViol)
                continue;
            afterCondClassified++;

            boolean lookupViol = false;
            if (lookup != null) {
                for (String cond : userConditions) {
                    if (lookup.hasContraindication(cond)) {
                        lookupViol = true;
                        break;
                    }
                }
            }
            if (lookupViol)
                continue;
            afterCondLookup++;

            if (hasHealthConditionNumericViolation(nutrition.getNutrition(), userConditions))
                continue;
            afterNumeric++;
        }

        System.out.println("  [DEBUG filter breakdown | slot=" + slotMealType
                + " | diet=" + userDiet + " | conditions=" + userConditions + "]");
        System.out.printf("    Total recipes         : %d%n", total);
        System.out.printf("    After meal type filter: %d (-%d)%n", afterMealType, total - afterMealType);
        System.out.printf("    After nutrition check : %d (-%d)%n", afterNutrition, afterMealType - afterNutrition);
        System.out.printf("    After diet lookup     : %d (-%d)%n", afterDietLookup, afterNutrition - afterDietLookup);
        System.out.printf("    After diet classified : %d (-%d, diet_classified_hits=%d)%n",
                afterDietClassified, afterDietLookup - afterDietClassified, dietClassifiedHits);
        System.out.printf("    After cond classified : %d (-%d)%n",
                afterCondClassified, afterDietClassified - afterCondClassified);
        for (Map.Entry<String, Integer> e : condClassifiedHits.entrySet()) {
            if (e.getValue() > 0)
                System.out.printf("      [%s] hard_forbidden hits: %d%n", e.getKey(), e.getValue());
        }
        System.out.printf("    After cond lookup     : %d (-%d)%n",
                afterCondLookup, afterCondClassified - afterCondLookup);
        System.out.printf("    After numeric thresh  : %d (PASSED)%n", afterNumeric);

        if (service != null && !userConditions.isEmpty()) {
            System.out.println("  [DEBUG sample blocked recipes — first 3 per condition]");
            for (String cond : userConditions) {
                int shown = 0;
                for (Recipe recipe : allRecipes) {
                    if (shown >= 3)
                        break;
                    List<String> hits = service.getHardForbiddenMatches(recipe.getParsedIngredients(), cond);
                    if (!hits.isEmpty()) {
                        System.out.printf("    cond=%-20s | id=%d %-40s | blocked_by=%s%n",
                                cond, recipe.getId(), "\"" + recipe.getName() + "\"", hits);
                        shown++;
                    }
                }
            }
        }
    }

    private static boolean hasHealthConditionNumericViolation(NutritionInfo n, List<String> conditions) {
        if (conditions.isEmpty() || n == null)
            return false;

        for (String condition : conditions) {
            switch (condition.toLowerCase()) {
                case "diabetes":

                    if (n.getSugarsG() > 0 && n.getSugarsG() > 10.0)
                        return true;
                    if (n.getTotalCarbsG() > 0 && n.getTotalCarbsG() > 45.0)
                        return true;
                    break;
                case "hypertension":
                    if (n.getSodiumMg() > 0 && n.getSodiumMg() > 600.0)
                        return true;
                    break;
                case "high_cholesterol":
                    if (n.getSaturatedFatG() > 0 && n.getSaturatedFatG() > 7.0)
                        return true;
                    if (n.getCholesterolMg() > 0 && n.getCholesterolMg() > 100.0)
                        return true;
                    break;
                case "kidney_disease":
                    if (n.getSodiumMg() > 0 && n.getSodiumMg() > 400.0)
                        return true;
                    if (n.getProteinG() > 0 && n.getProteinG() > 20.0)
                        return true;
                    break;
                case "gout":
                    if (n.getProteinG() > 0 && n.getProteinG() > 25.0)
                        return true;
                    break;
            }
        }
        return false;
    }

    public static boolean hasAllergyViolation(Recipe recipe, List<String> allergies) {
        if (allergies.isEmpty())
            return false;

        String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";

        for (String allergy : allergies) {
            String allergyNorm = allergy.toLowerCase().replace("_", " ").trim();

            List<String> allergyKeywords = getAllergyKeywords(allergyNorm);

            for (String keyword : allergyKeywords) {

                if (containsWordBoundary(recipeName, keyword))
                    return true;

                for (String ing : recipe.getParsedIngredients()) {
                    String ingLower = ing.toLowerCase();

                    if (containsWordBoundary(ingLower, keyword))
                        return true;
                }
            }
        }
        return false;
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    static boolean containsWordBoundary(String text, String keyword) {
        if (text == null || keyword == null)
            return false;
        Pattern p = PATTERN_CACHE.computeIfAbsent(
                keyword, k -> Pattern.compile("\\b" + Pattern.quote(k) + "\\b"));
        return p.matcher(text).find();
    }

    private static String toSingularSafe(String allergy) {
        switch (allergy) {
            case "peanuts":
                return "peanut";
            case "tree_nuts":
                return "tree_nut";
            case "nuts":
                return "nut";
            case "eggs":
                return "egg";
            case "soybeans":
                return "soybean";

            default:
                return allergy;
        }
    }

    static List<String> getAllergyKeywords(String allergy) {
        List<String> keywords = new ArrayList<>();

        String normalized = toSingularSafe(allergy);
        keywords.add(normalized);

        if (!normalized.equals(allergy)) {
            keywords.add(allergy);
        }

        switch (allergy.replace(" ", "_")) {
            case "peanut":
            case "peanuts":
                keywords.add("peanut");
                keywords.add("groundnut");
                keywords.add("arachis");
                keywords.add("monkey nut");
                break;
            case "tree_nut":
            case "tree_nuts":
            case "nut":
            case "nuts":
                keywords.add("almond");
                keywords.add("cashew");
                keywords.add("walnut");
                keywords.add("pecan");
                keywords.add("pistachio");
                keywords.add("hazelnut");
                keywords.add("macadamia");
                keywords.add("pine nut");
                keywords.add("brazil nut");
                keywords.add("chestnut");
                break;
            case "shellfish":
                keywords.add("shrimp");
                keywords.add("prawn");
                keywords.add("crab");
                keywords.add("lobster");
                keywords.add("crayfish");
                keywords.add("clam");
                keywords.add("oyster");
                keywords.add("scallop");
                keywords.add("mussel");
                keywords.add("squid");
                keywords.add("octopus");
                break;
            case "fish":
                keywords.add("salmon");
                keywords.add("tuna");
                keywords.add("cod");
                keywords.add("tilapia");
                keywords.add("halibut");
                keywords.add("bass");
                keywords.add("anchovy");
                keywords.add("anchovies");
                keywords.add("sardine");
                keywords.add("mackerel");
                keywords.add("trout");
                keywords.add("herring");
                break;
            case "dairy":
            case "milk":
                keywords.add("milk");
                keywords.add("cheese");
                keywords.add("butter");
                keywords.add("cream");
                keywords.add("yogurt");
                keywords.add("whey");
                keywords.add("casein");
                keywords.add("lactose");
                break;
            case "egg":
            case "eggs":
                keywords.add("egg");
                keywords.add("eggs");
                keywords.add("mayonnaise");
                keywords.add("meringue");
                keywords.add("albumin");
                break;
            case "soy":
            case "soya":
                keywords.add("soy");
                keywords.add("soya");
                keywords.add("tofu");
                keywords.add("edamame");
                keywords.add("tempeh");
                keywords.add("miso");
                keywords.add("soy sauce");
                break;
            case "wheat":
            case "gluten":
                keywords.add("wheat");
                keywords.add("flour");
                keywords.add("gluten");
                keywords.add("barley");
                keywords.add("rye");
                break;
        }
        return keywords;
    }

    public static boolean hasDislikedIngredient(Recipe recipe, List<String> disliked) {
        if (disliked.isEmpty())
            return false;
        String recipeName = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
        for (String d : disliked) {
            String dNorm = d.replace("_", " ").toLowerCase();
            String searchKeyword = dNorm.equals("fried food") ? "fried" : dNorm;

            String searchKeywordSingular = searchKeyword.endsWith("s") && searchKeyword.length() > 3
                    ? searchKeyword.substring(0, searchKeyword.length() - 1)
                    : searchKeyword;

            if (containsWordBoundary(recipeName, searchKeyword))
                return true;

            for (String ingredientName : recipe.getParsedIngredients()) {
                if (containsWordBoundary(ingredientName.toLowerCase(), searchKeywordSingular))
                    return true;
            }
        }
        return false;
    }

    private static final Set<String> BANNED_MEAL_TYPES_FOR_MAIN = new HashSet<>(
            Arrays.asList("dessert", "snack", "appetizers"));

    private static final List<String> BANNED_NAME_KEYWORDS = Arrays.asList(
            "smoothie", "ice cream", "gelato", "sorbet", "chips", "trail mix",
            "chocolate", "jelly", "jello", "jells", "cookie", "brownie",
            "candy", "fudge", "milkshake", "popsicle", "granola bar",
            "banana split", "fruit dip", "jam recipe", "granola",
            "cake", "pudding", "parfait", "mousse", "cream puff",
            "donut", "doughnut", "muffin", "caramel", "truffle",
            "popcorn", "marshmallow", "meringue");

    private static final Set<String> DESSERT_TAGS = new HashSet<>(Arrays.asList(
            "dessert", "desserts", "sweet", "candy", "cookies-and-brownies",
            "cakes", "pies", "ice-cream"));

    private static final List<String> COMPONENT_SLOT_BAN_KEYWORDS = Arrays.asList(

            " sauce", "glaze", " topping", "frosting", "icing", "dressing",
            "marinade", "vinaigrette",

            "pesto", "guacamole", "hummus", "tapenade",

            " dough", "pie crust", "pizza crust", "pasta dough",
            "bread dough", "tortilla dough",

            "simple syrup", "meringue topping", "whipped cream");

    private static final List<String> SIDE_DISH_KEYWORDS = Arrays.asList(
            "roast potatoes", "roasted potatoes", "candied yams", "glazed carrots",
            "glazed parsnip", "mashed potatoes", "steamed rice", "plain rice",
            "buttered noodles", "sauteed mushrooms", "sauteed spinach",
            "steamed broccoli", "steamed vegetables", "roasted vegetables",
            "roasted asparagus", "grilled asparagus", "sauteed asparagus");

    static boolean isBannedFromMainMeal(Recipe recipe, String slotMealType) {
        String nameLower = recipe.getName() != null ? recipe.getName().toLowerCase() : "";
        String rt = recipe.getMealType() != null ? recipe.getMealType().toLowerCase() : "";

        for (String kw : COMPONENT_SLOT_BAN_KEYWORDS) {
            if (nameLower.contains(kw)) {

                boolean hasProtein = nameLower.contains("chicken") || nameLower.contains("beef")
                        || nameLower.contains("pork") || nameLower.contains("fish")
                        || nameLower.contains("salmon") || nameLower.contains("turkey")
                        || nameLower.contains("shrimp") || nameLower.contains("lamb");
                if (!hasProtein)
                    return true;
            }
        }

        if ("breakfast".equals(slotMealType)) {

            for (String tag : recipe.getTags()) {
                String t = tag.toLowerCase();
                if ("main-dish".equals(t) && (nameLower.contains("roast") || nameLower.contains("stew")
                        || nameLower.contains("casserole") && !nameLower.contains("egg")
                                && !nameLower.contains("breakfast"))) {
                    return true;
                }
            }

            for (String kw : Arrays.asList(" sauce", "glaze", " topping", "frosting",
                    "icing", "vinaigrette", "dressing", "marinade")) {
                if (nameLower.contains(kw))
                    return true;
            }
            return false; 
        }

        if (!"lunch".equals(slotMealType) && !"dinner".equals(slotMealType))
            return false;

        if (BANNED_MEAL_TYPES_FOR_MAIN.contains(rt))
            return true;

        for (String keyword : BANNED_NAME_KEYWORDS) {
            if (nameLower.contains(keyword))
                return true;
        }

        for (String tag : recipe.getTags()) {
            if (DESSERT_TAGS.contains(tag.toLowerCase()))
                return true;
        }

        for (String kw : SIDE_DISH_KEYWORDS) {
            if (nameLower.contains(kw))
                return true;
        }

        return false;
    }

    private static final Set<String> GLUTEN_SAFE_INGREDIENTS = new HashSet<>(Arrays.asList(
            "corn tortilla",
            "corn flour",
            "rice flour",
            "rice bread",
            "gluten-free bread",
            "gluten-free flour",
            "gluten-free pasta",
            "gluten-free noodle",
            "gluten-free cracker",
            "gluten-free soy sauce",
            "tamari", 
            "rice noodle",
            "rice paper",
            "cornbread", 
            "corn starch",
            "arrowroot",

            "white miso",
            "yellow miso",
            "red miso",
            "rice miso",
            "shiro miso", 
            "sweet miso"));

    private static final List<String> GLUTEN_KEYWORDS = Arrays.asList(

            "wheat", "flour", "barley", "rye", "spelt", "kamut", "triticale",

            "bread", "toast", "croissant", "baguette", "pita", "pretzel",

            "pasta", "spaghetti", "macaroni", "noodle", "couscous", "orzo",
            "fettuccine", "linguine", "penne", "lasagna", "ravioli", "gnocchi",

            "semolina", "bulgur", "pastry", "crouton", "cracker", "breadcrumb",
            "panko", "tortilla", "filo", "phyllo", "dumpling", "wonton",

            "soy sauce", 
            "teriyaki", 
            "mugi miso", 
            "barley miso", 

            "seitan", 
            "wheat germ",
            "wheat bran",
            "malt", 
            "malt vinegar");

    static boolean containsGlutenIngredient(Recipe recipe) {
        for (String ingredient : recipe.getParsedIngredients()) {
            String ingLower = ingredient.toLowerCase().trim();

            boolean isSafe = false;
            for (String safe : GLUTEN_SAFE_INGREDIENTS) {
                if (ingLower.contains(safe)) {
                    isSafe = true;
                    break;
                }
            }
            if (isSafe)
                continue;

            for (String keyword : GLUTEN_KEYWORDS) {
                if (ingLower.contains(keyword))
                    return true;
            }
        }
        return false;
    }

    public static boolean exceedsComplexity(String recipeComplexity, String userComplexity) {
        if ("ANY".equalsIgnoreCase(userComplexity))
            return false;
        return complexityLevel(recipeComplexity) > complexityLevel(userComplexity);
    }

    public static boolean exceedsBudget(String recipeBudget, String userBudget) {
        return budgetLevel(recipeBudget) > budgetLevel(userBudget);
    }

    public static boolean exceedsComplexityByMoreThanOne(String recipeComplexity, String userComplexity) {
        if ("ANY".equalsIgnoreCase(userComplexity))
            return false;
        return complexityLevel(recipeComplexity) - complexityLevel(userComplexity) > 1;
    }

    public static boolean exceedsBudgetByMoreThanOne(String recipeBudget, String userBudget) {
        return budgetLevel(recipeBudget) - budgetLevel(userBudget) > 1;
    }

    public static int complexityLevel(String complexity) {
        if (complexity == null)
            return 0;
        switch (complexity.toLowerCase()) {
            case "easy":
                return 1;
            case "medium":
                return 2;
            case "hard":
                return 3;
            default:
                return 0;
        }
    }

    public static int budgetLevel(String budget) {
        if (budget == null)
            return 0;
        switch (budget.toLowerCase()) {
            case "low":
                return 1;
            case "medium":
                return 2;
            case "high":
                return 3;
            default:
                return 0;
        }
    }

    private static boolean matchesMealType(String recipeMealType, String slotMealType) {
        if (recipeMealType == null)
            return false;
        String rt = recipeMealType.toLowerCase();
        String st = slotMealType.toLowerCase();
        if (rt.equals(st))
            return true;
        if ("snack".equals(st) && ("dessert".equals(rt) || "appetizers".equals(rt)))
            return true;
        return false;
    }

    public static boolean matchesAdjacentMealType(String recipeMealType, String slotMealType) {
        if (recipeMealType == null)
            return false;
        String rt = recipeMealType.toLowerCase();
        String st = slotMealType.toLowerCase();
        if (matchesMealType(recipeMealType, slotMealType))
            return true;
        switch (st) {
            case "breakfast":
                return "snack".equals(rt);
            case "lunch":
                return "dinner".equals(rt);
            case "dinner":
                return "lunch".equals(rt);
            case "snack":
                return "dessert".equals(rt) || "breakfast".equals(rt);
            default:
                return false;
        }
    }

    public static class FilterResult {
        private final List<Recipe> recipes;
        private final String filteringNote;
        private final Map<String, Integer> eliminationCounts;

        public FilterResult(List<Recipe> recipes, String filteringNote,
                Map<String, Integer> eliminationCounts) {
            this.recipes = recipes;
            this.filteringNote = filteringNote;
            this.eliminationCounts = eliminationCounts;
        }

        public List<Recipe> getRecipes() {
            return recipes;
        }

        public String getFilteringNote() {
            return filteringNote;
        }

        public Map<String, Integer> getEliminationCounts() {
            return eliminationCounts;
        }
    }
}