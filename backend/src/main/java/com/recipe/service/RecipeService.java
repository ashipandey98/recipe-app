package com.recipe.service;

import com.google.gson.*;
import com.recipe.entity.Recipe;
import com.recipe.entity.MealPlan;
import com.recipe.repository.RecipeRepository;
import com.recipe.repository.MealPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Transactional
public class RecipeService {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    private final Gson gson = new Gson();

    /**
     * Generate recipes from ingredients
     */
    public List<Map<String, Object>> generateRecipes(String ingredients, String cuisine, String maxTime) {
        log.info("Generating recipes for ingredients: {}", ingredients);

        String jsonResponse = geminiService.generateRecipes(ingredients, cuisine, maxTime);
        log.debug("Gemini raw response: {}", jsonResponse);

        try {
            JsonArray recipesArray = JsonParser.parseString(jsonResponse).getAsJsonArray();
            List<Map<String, Object>> recipes = new ArrayList<>();

            for (JsonElement element : recipesArray) {
                JsonObject obj = element.getAsJsonObject();
                Map<String, Object> recipe = new HashMap<>();

                recipe.put("name", getStr(obj, "name"));
                recipe.put("cookingTime", getStr(obj, "cookingTime"));
                recipe.put("cuisine", getStr(obj, "cuisine"));
                recipe.put("calories", getStr(obj, "calories"));
                recipe.put("tip", getStr(obj, "tip"));

                // Parse ingredients list
                List<String> ingredientsList = new ArrayList<>();
                if (obj.has("ingredients")) {
                    obj.getAsJsonArray("ingredients")
                            .forEach(i -> ingredientsList.add(i.getAsString()));
                }
                recipe.put("ingredients", ingredientsList);

                // Parse instructions list
                List<String> instructionsList = new ArrayList<>();
                if (obj.has("instructions")) {
                    obj.getAsJsonArray("instructions")
                            .forEach(i -> instructionsList.add(i.getAsString()));
                }
                recipe.put("instructions", instructionsList);
                recipe.put("inputIngredients", ingredients);

                recipes.add(recipe);
            }

            log.info("Generated {} recipes", recipes.size());
            return recipes;

        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
            return List.of(Map.of(
                    "name", "Error generating recipes",
                    "cookingTime", "N/A",
                    "cuisine", "N/A",
                    "calories", "N/A",
                    "ingredients", List.of(),
                    "instructions", List.of("Please check your Gemini API key and try again."),
                    "tip", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Save a recipe the user liked
     */
    public Recipe saveRecipe(String name, String ingredients, String instructions,
                             String cookingTime, String cuisine, String calories,
                             String inputIngredients) {

        Recipe recipe = Recipe.builder()
                .name(name)
                .ingredients(ingredients)
                .instructions(instructions)
                .cookingTime(cookingTime)
                .cuisine(cuisine)
                .calories(calories)
                .inputIngredients(inputIngredients)
                .saved(true)
                .build();

        Recipe saved = recipeRepository.save(recipe);
        log.info("Recipe saved: {} (ID: {})", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Get all saved recipes
     */
    public List<Recipe> getSavedRecipes() {
        return recipeRepository.findBySavedTrueOrderByCreatedAtDesc();
    }

    /**
     * Delete a saved recipe
     */
    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
        log.info("Recipe deleted: {}", id);
    }

    /**
     * Generate a 2-week meal plan
     */
    public Map<String, Object> generateMealPlan(String preferences) {
        log.info("Generating 2-week meal plan with preferences: {}", preferences);

        String jsonResponse = geminiService.generateMealPlan(preferences);

        try {
            JsonObject planObj = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Save to database
            MealPlan mealPlan = MealPlan.builder()
                    .planContent(jsonResponse)
                    .emailSent(false)
                    .build();
            mealPlanRepository.save(mealPlan);

            // Return parsed plan
            return parseMealPlan(planObj);

        } catch (Exception e) {
            log.error("Error parsing meal plan", e);
            return Map.of("error", "Failed to generate meal plan: " + e.getMessage());
        }
    }

    /**
     * Get the latest meal plan
     */
    public Map<String, Object> getLatestMealPlan() {
        return mealPlanRepository.findTopByOrderByCreatedAtDesc()
                .map(plan -> {
                    try {
                        JsonObject obj = JsonParser.parseString(plan.getPlanContent()).getAsJsonObject();
                        Map<String, Object> result = parseMealPlan(obj);
                        result.put("id", plan.getId());
                        result.put("createdAt", plan.getCreatedAt().toString());
                        result.put("emailSent", plan.isEmailSent());
                        return result;
                    } catch (Exception e) {
                        return Map.<String, Object>of("error", "Could not parse meal plan");
                    }
                })
                .orElse(Map.of("message", "No meal plan found. Generate one first."));
    }

    /**
     * Parse meal plan JSON into a clean map
     */
    private Map<String, Object> parseMealPlan(JsonObject planObj) {
        Map<String, Object> result = new HashMap<>();

        // Week 1
        if (planObj.has("week1")) {
            result.put("week1", parseDays(planObj.getAsJsonArray("week1")));
        }

        // Week 2
        if (planObj.has("week2")) {
            result.put("week2", parseDays(planObj.getAsJsonArray("week2")));
        }

        // Shopping list
        if (planObj.has("shoppingList")) {
            JsonObject shoppingObj = planObj.getAsJsonObject("shoppingList");
            Map<String, List<String>> shopping = new HashMap<>();

            if (shoppingObj.has("week1")) {
                List<String> w1 = new ArrayList<>();
                shoppingObj.getAsJsonArray("week1").forEach(i -> w1.add(i.getAsString()));
                shopping.put("week1", w1);
            }
            if (shoppingObj.has("week2")) {
                List<String> w2 = new ArrayList<>();
                shoppingObj.getAsJsonArray("week2").forEach(i -> w2.add(i.getAsString()));
                shopping.put("week2", w2);
            }
            result.put("shoppingList", shopping);
        }

        // Nutrition tips
        if (planObj.has("nutritionTips")) {
            List<String> tips = new ArrayList<>();
            planObj.getAsJsonArray("nutritionTips").forEach(t -> tips.add(t.getAsString()));
            result.put("nutritionTips", tips);
        }

        return result;
    }

    private List<Map<String, String>> parseDays(JsonArray daysArray) {
        List<Map<String, String>> days = new ArrayList<>();
        for (JsonElement element : daysArray) {
            JsonObject day = element.getAsJsonObject();
            Map<String, String> dayMap = new HashMap<>();
            dayMap.put("day", getStr(day, "day"));
            dayMap.put("breakfast", getStr(day, "breakfast"));
            dayMap.put("lunch", getStr(day, "lunch"));
            dayMap.put("dinner", getStr(day, "dinner"));
            days.add(dayMap);
        }
        return days;
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "";
    }
}
