import model.Recipe;
import model.UserProfile;
import model.output.WeeklyMealPlan;
import service.CalorieCalculator;
import service.DataLoader;
import service.MealPlanGenerator;
import service.MealPlanGeneratorVariant2;
import service.RecipeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Main {

    public static void main(String[] args) {
        try {

            String resourcesDir = Paths.get("src", "resources").toAbsolutePath().toString();
            String outputDir = Paths.get(resourcesDir, "output").toAbsolutePath().toString();

            System.out.println("=== Weekly Meal Plan Generator ===");
            System.out.println("Resources: " + resourcesDir);
            System.out.println();

            System.out.println("[Loading data...]");
            DataLoader dataLoader = new DataLoader(resourcesDir);
            dataLoader.loadAll();
            System.out.println();

            MealPlanGeneratorVariant2 generator = new MealPlanGeneratorVariant2(dataLoader);

            int index = 0;
            for (UserProfile user : dataLoader.getUserProfiles()) {
                index++;
                System.out.println("==============================================");
                System.out.println("[User " + index + ": " + user.getUserId() + "]");
                System.out.println("  Gender: " + user.getGender()
                        + ", Age: " + user.getAge()
                        + ", Height: " + user.getHeightCm() + " cm"
                        + ", Weight: " + user.getWeightKg() + " kg");
                System.out.println("  Activity: " + user.getActivityLevel()
                        + ", Goal: " + user.getGoal()
                        + " (" + user.getGoalIntensity() + ")");
                System.out.println("  Diet: " + user.getDietType()
                        + ", Meals/day: " + user.getMealsPerDay()
                        + ", Zigzag: " + user.isZigzag());

                double bmr = CalorieCalculator.calculateBMR(user);
                double tdee = CalorieCalculator.calculateTDEE(user);
                double adjusted = CalorieCalculator.adjustForGoal(user);
                double[] dailyTargets = CalorieCalculator.getDailyTargets(user);
                double weeklyTotal = CalorieCalculator.getWeeklyTotal(dailyTargets);

                System.out.printf("  BMR: %.1f kcal | TDEE: %.1f kcal | Adjusted: %.1f kcal%n",
                        bmr, tdee, adjusted);
                System.out.printf("  Weekly total: %.0f kcal%n", weeklyTotal);
                System.out.print("  Daily targets: ");
                for (int i = 0; i < 7; i++) {
                    System.out.printf("D%d=%.0f ", i + 1, dailyTargets[i]);
                }
                System.out.println();

                System.out.println("  [Generating meal plan...]");
                WeeklyMealPlan plan = generator.generatePlan(user);

                for (var dayPlan : plan.getDays()) {
                    System.out.println("  Day " + dayPlan.getDay()
                            + " (" + dayPlan.getDailyCalorieTarget() + " kcal):");
                    for (var slot : dayPlan.getSlots()) {
                        String coverage = slot.isLowCoverage() ? " [LOW COVERAGE]" : "";
                        System.out.println("    " + slot.getMealType()
                                + " (" + slot.getSlotCalorieBudget() + " kcal) → "
                                + slot.getCandidates().size() + " candidates" + coverage);

                        int shown = 0;
                        for (var candidate : slot.getCandidates()) {
                            if (shown >= 3)
                                break;
                            System.out.printf("      #%d: %s (score=%d, cal=%.0f, dev=%.1f%%)%n",
                                    shown + 1, candidate.getRecipeName(),
                                    candidate.getScore(), candidate.getCaloriesPerServing(),
                                    candidate.getCaloricDeviation());
                            shown++;
                        }
                    }
                }

                MealPlanGeneratorVariant2.writeOutput(plan, outputDir, index);
                System.out.println();
            }

            writeUserRecipeCounts(dataLoader.getUserProfiles(), dataLoader, outputDir);

            System.out.println("=== Done! Generated " + index + " meal plans. ===");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void writeUserRecipeCounts(List<UserProfile> users, DataLoader dl, String outputDir)
            throws IOException {
        Map<String, Map<String, Object>> output = new HashMap<>();
        for (UserProfile user : users) {
            Map<String, Object> userData = new HashMap<>();

            RecipeFilter.FilterResult overallRes = RecipeFilter.filterRecipes(dl.getRecipes(), user, dl, "ALL");

            Set<Integer> validRecipes = new HashSet<>();
            for (String mealType : Arrays.asList("breakfast", "lunch", "dinner", "snack")) {
                RecipeFilter.FilterResult res = RecipeFilter.filterRecipes(dl.getRecipes(), user, dl, mealType);
                for (Recipe r : res.getRecipes()) {
                    validRecipes.add(r.getId());
                }
            }

            userData.put("valid_recipes", validRecipes.size());
            userData.put("eliminations", overallRes.getEliminationCounts());
            output.put(user.getUserId(), userData);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(output);
        java.nio.file.Path outputPath = Paths.get(outputDir, "user_recipe_counts.json");
        Files.write(outputPath, json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Written user recipe counts to: " + outputPath.toAbsolutePath());
    }
}
