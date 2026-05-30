package com.recipe.service;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GeminiService {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.url}")
    private String apiUrl;

    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public GeminiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public String generateRecipes(String ingredients, String cuisine, String maxTime) {
        String prompt = String.format("""
                You are a helpful chef. Generate exactly 3 recipes using ONLY these ingredients: %s
                Cuisine preference: %s
                Max cooking time: %s
                Basic pantry items like salt, oil, water are allowed.

                Return ONLY a valid JSON array with exactly this structure, no extra text:
                [
                  {
                    "name": "Recipe Name",
                    "cookingTime": "20 mins",
                    "cuisine": "Indian",
                    "calories": "350 kcal",
                    "ingredients": ["ingredient 1", "ingredient 2"],
                    "instructions": ["Step 1: ...", "Step 2: ...", "Step 3: ..."],
                    "tip": "A helpful cooking tip"
                  }
                ]
                """, ingredients, cuisine, maxTime);

        return callGroq(prompt);
    }

    public String generateMealPlan(String preferences) {
        String prompt = String.format("""
                You are a nutritionist chef. Create a detailed 2-week meal plan.
                Preferences: %s

                Return ONLY a valid JSON object with exactly this structure, no extra text:
                {
                  "week1": [
                    {
                      "day": "Monday",
                      "breakfast": "Meal name",
                      "lunch": "Meal name",
                      "dinner": "Meal name"
                    }
                  ],
                  "week2": [
                    {
                      "day": "Monday",
                      "breakfast": "Meal name",
                      "lunch": "Meal name",
                      "dinner": "Meal name"
                    }
                  ],
                  "shoppingList": {
                    "week1": ["item 1 (quantity)", "item 2 (quantity)"],
                    "week2": ["item 1 (quantity)", "item 2 (quantity)"]
                  },
                  "nutritionTips": ["tip 1", "tip 2", "tip 3"]
                }
                Include all 7 days for each week.
                """, preferences);

        return callGroq(prompt);
    }

    private String callGroq(String prompt) {
        try {
            // Groq uses OpenAI-compatible format
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "llama-3.3-70b-versatile");
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2048);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            requestBody.add("messages", messages);

            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null
                            ? response.body().string() : "No error body";
                    log.error("Groq API error: {} - {}", response.code(), errorBody);
                    return getFallbackResponse();
                }

                String responseBody = response.body().string();
                JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);

                // Extract text from OpenAI-compatible response
                String text = responseJson
                        .getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString();

                // Clean markdown if present
                text = text.trim();
                if (text.startsWith("```json")) text = text.substring(7);
                if (text.startsWith("```"))     text = text.substring(3);
                if (text.endsWith("```"))       text = text.substring(0, text.length() - 3);

                return text.trim();
            }

        } catch (IOException e) {
            log.error("Error calling Groq API", e);
            return getFallbackResponse();
        }
    }

    private String getFallbackResponse() {
        return """
                [
                  {
                    "name": "Simple Stir Fry",
                    "cookingTime": "15 mins",
                    "cuisine": "Any",
                    "calories": "300 kcal",
                    "ingredients": ["whatever you have"],
                    "instructions": ["Mix everything", "Cook on medium heat", "Season and serve"],
                    "tip": "Check your Groq API key in application.yml"
                  }
                ]
                """;
    }
}