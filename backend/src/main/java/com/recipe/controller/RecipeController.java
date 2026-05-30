package com.recipe.controller;

import com.recipe.entity.Recipe;
import com.recipe.service.EmailService;
import com.recipe.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@Slf4j
@CrossOrigin(origins = "*")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private EmailService emailService;

    // ─── Health Check ────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Recipe API is running!"));
    }

    // ─── Recipe Generation ────────────────────────────────────────────────────

    /**
     * Generate recipes from ingredients
     * POST /api/v1/recipes/generate
     * Body: { "ingredients": "rice, eggs, onion", "cuisine": "Any", "maxTime": "30 mins" }
     */
    @PostMapping("/recipes/generate")
    public ResponseEntity<?> generateRecipes(@RequestBody Map<String, String> request) {
        String ingredients = request.getOrDefault("ingredients", "");
        String cuisine = request.getOrDefault("cuisine", "Any");
        String maxTime = request.getOrDefault("maxTime", "30 mins");

        if (ingredients.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please provide ingredients"));
        }

        log.info("Generating recipes for: {}", ingredients);
        List<Map<String, Object>> recipes = recipeService.generateRecipes(ingredients, cuisine, maxTime);
        return ResponseEntity.ok(Map.of("recipes", recipes, "count", recipes.size()));
    }

    // ─── Saved Recipes ────────────────────────────────────────────────────────

    /**
     * Save a recipe
     * POST /api/v1/recipes/save
     */
    @PostMapping("/recipes/save")
    public ResponseEntity<?> saveRecipe(@RequestBody Map<String, Object> request) {
        try {
            Recipe saved = recipeService.saveRecipe(
                    (String) request.get("name"),
                    request.get("ingredients").toString(),
                    request.get("instructions").toString(),
                    (String) request.getOrDefault("cookingTime", ""),
                    (String) request.getOrDefault("cuisine", ""),
                    (String) request.getOrDefault("calories", ""),
                    (String) request.getOrDefault("inputIngredients", "")
            );
            return ResponseEntity.ok(Map.of(
                    "message", "Recipe saved!",
                    "id", saved.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all saved recipes
     * GET /api/v1/recipes/saved
     */
    @GetMapping("/recipes/saved")
    public ResponseEntity<?> getSavedRecipes() {
        List<Recipe> recipes = recipeService.getSavedRecipes();
        return ResponseEntity.ok(Map.of("recipes", recipes, "count", recipes.size()));
    }

    /**
     * Delete a saved recipe
     * DELETE /api/v1/recipes/{id}
     */
    @DeleteMapping("/recipes/{id}")
    public ResponseEntity<?> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok(Map.of("message", "Recipe deleted"));
    }

    // ─── Meal Plan ────────────────────────────────────────────────────────────

    /**
     * Generate a 2-week meal plan
     * POST /api/v1/menu/generate
     * Body: { "preferences": "vegetarian, low-carb" }
     */
    @PostMapping("/menu/generate")
    public ResponseEntity<?> generateMealPlan(@RequestBody(required = false) Map<String, String> request) {
        String preferences = request != null
                ? request.getOrDefault("preferences", "balanced and healthy")
                : "balanced and healthy";

        log.info("Generating 2-week meal plan with preferences: {}", preferences);
        Map<String, Object> plan = recipeService.generateMealPlan(preferences);
        return ResponseEntity.ok(plan);
    }

    /**
     * Get the latest meal plan
     * GET /api/v1/menu/latest
     */
    @GetMapping("/menu/latest")
    public ResponseEntity<?> getLatestMealPlan() {
        return ResponseEntity.ok(recipeService.getLatestMealPlan());
    }

    /**
     * Send meal plan email manually
     * POST /api/v1/menu/send-email
     * Body: { "email": "user@gmail.com" }
     */
    @PostMapping("/menu/send-email")
    public ResponseEntity<?> sendMealPlanEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        // Get latest plan
        Map<String, Object> plan = recipeService.getLatestMealPlan();

        if (plan.containsKey("message")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No meal plan found. Generate one first."));
        }

        boolean sent = emailService.sendMealPlanEmail(email, plan);

        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "message", "Meal plan sent to " + email + "!",
                    "success", true
            ));
        } else {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to send email. Check your Gmail App Password.",
                    "success", false
            ));
        }
    }
}
