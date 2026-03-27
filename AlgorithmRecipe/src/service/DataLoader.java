package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DataLoader {

    private List<Recipe> recipes;
    private Map<Integer, NutritionEntry> nutritionMap;
    private Map<String, Ingredient> ingredientIndex;
    private Map<String, Tag> tagIndex;
    private DietaryReference dietaryReference;
    private List<UserProfile> userProfiles;

    private IngredientLookupService ingredientLookupService;
    private ClassifiedIngredientsService classifiedIngredientsService;
    private Map<Integer, IngredientLookupService.IngredientLookupResult> lookupCache;

    private final String resourcesDir;

    public DataLoader(String resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public void loadAll() throws IOException {
        Gson gson = new Gson();

        String recipesJson = readFile("recipes.json");
        Type recipeListType = new TypeToken<List<Recipe>>() {
        }.getType();
        recipes = gson.fromJson(recipesJson, recipeListType);
        for (Recipe recipe : recipes) {
            recipe.parseIngredients();
        }
        System.out.println("  Loaded " + recipes.size() + " recipes");

        String nutritionJson = readFile("nutrition_data_formatted.json");
        Type nutritionMapType = new TypeToken<Map<String, NutritionEntry>>() {
        }.getType();
        Map<String, NutritionEntry> rawNutritionMap = gson.fromJson(nutritionJson, nutritionMapType);
        nutritionMap = new HashMap<>();
        for (Map.Entry<String, NutritionEntry> entry : rawNutritionMap.entrySet()) {
            try {
                int id = Integer.parseInt(entry.getKey());
                nutritionMap.put(id, entry.getValue());
            } catch (NumberFormatException e) {
            }
        }
        System.out.println("  Loaded " + nutritionMap.size() + " nutrition entries");

        String ingredientsJson = readFile("ingredients.json");
        Type ingredientListType = new TypeToken<List<Ingredient>>() {
        }.getType();
        List<Ingredient> ingredientList = gson.fromJson(ingredientsJson, ingredientListType);
        ingredientIndex = new HashMap<>();
        for (Ingredient ing : ingredientList) {
            ingredientIndex.put(ing.getName().toLowerCase(), ing);
        }
        System.out.println("  Loaded " + ingredientIndex.size() + " ingredients");

        String tagsJson = readFile("unique_tags.json");
        Type tagListType = new TypeToken<List<Tag>>() {
        }.getType();
        List<Tag> tagList = gson.fromJson(tagsJson, tagListType);
        tagIndex = new HashMap<>();
        for (Tag tag : tagList) {
            tagIndex.put(tag.getName().toLowerCase(), tag);
        }
        System.out.println("  Loaded " + tagIndex.size() + " tags");

        String dietaryJson = readFile("dietary_reference.json");
        dietaryReference = gson.fromJson(dietaryJson, DietaryReference.class);
        System.out.println("  Loaded dietary reference: " +
                dietaryReference.getContraindications().size() + " conditions, " +
                dietaryReference.getDiets().size() + " diets");

        String surveyJson = readFile("survey.json");
        Type userListType = new TypeToken<List<UserProfile>>() {
        }.getType();
        userProfiles = gson.fromJson(surveyJson, userListType);
        System.out.println("  Loaded " + userProfiles.size() + " user profiles");

        System.out.println("  [Loading ingredient tags + normalization map...]");
        String normMapJson = readFile("ingredient_normalization_map_extended.json");
        Type normMapType = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> normalizationMap = gson.fromJson(normMapJson, normMapType);

        String ingredientTagsJson = readFile("ingredient_tags_fixed.json");
        Type tagsMapType = new TypeToken<Map<String, Map<String, String>>>() {
        }.getType();
        Map<String, Map<String, String>> ingredientTagsMap = gson.fromJson(ingredientTagsJson, tagsMapType);

        classifiedIngredientsService = new ClassifiedIngredientsService(normalizationMap, ingredientTagsMap);
        System.out.println("  Loaded normalization map: " + normalizationMap.size() + " entries, "
                + "ingredient tags: " + ingredientTagsMap.size() + " ingredients, "
                + "conditions: " + classifiedIngredientsService.getConditionNames().size());

        System.out.println("  [Building ingredient lookup service...]");
        ingredientLookupService = new IngredientLookupService(ingredientList);

        lookupCache = new HashMap<>();
        int recipesWithContra = 0;
        int recipesWithDietRestrictions = 0;
        for (Recipe recipe : recipes) {
            IngredientLookupService.IngredientLookupResult result = ingredientLookupService
                    .resolveRecipe(recipe.getParsedIngredients());
            lookupCache.put(recipe.getId(), result);
            if (!result.getContraindications().isEmpty())
                recipesWithContra++;
            if (!result.getNotSuitableForDiets().isEmpty())
                recipesWithDietRestrictions++;
        }
        System.out.println("  Ingredient lookup: " + recipesWithContra
                + " recipes have contraindications, " + recipesWithDietRestrictions
                + " have diet restrictions");
    }

    private String readFile(String filename) throws IOException {
        Path path = Paths.get(resourcesDir, filename);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public List<Recipe> getRecipes() {
        return recipes;
    }

    public Map<Integer, NutritionEntry> getNutritionMap() {
        return nutritionMap;
    }

    public NutritionEntry getNutrition(int recipeId) {
        return nutritionMap.get(recipeId);
    }

    public Ingredient getIngredient(String name) {
        return ingredientIndex.get(name.toLowerCase());
    }

    public Tag getTag(String name) {
        return tagIndex.get(name.toLowerCase());
    }

    public Map<String, Ingredient> getIngredientIndex() {
        return ingredientIndex;
    }

    public Map<String, Tag> getTagIndex() {
        return tagIndex;
    }

    public DietaryReference getDietaryReference() {
        return dietaryReference;
    }

    public List<UserProfile> getUserProfiles() {
        return userProfiles;
    }

    public IngredientLookupService.IngredientLookupResult getLookupResult(int recipeId) {
        return lookupCache.get(recipeId);
    }

    public Map<Integer, IngredientLookupService.IngredientLookupResult> getLookupCache() {
        return lookupCache;
    }

    public ClassifiedIngredientsService getClassifiedIngredientsService() {
        return classifiedIngredientsService;
    }

    public List<String> getEffectiveTags(Recipe recipe) {
        return new ArrayList<>(recipe.getTags());
    }
}