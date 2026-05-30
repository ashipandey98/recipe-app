package com.recipe.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class MenuSchedulerService {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private EmailService emailService;

    @Value("${app.menu.recipient-email}")
    private String recipientEmail;

    /**
     * Runs every 2 weeks automatically.
     * Generates a fresh meal plan and emails it.
     *
     * Cron: "0 0 8 1/14 * *"
     * = At 8:00 AM every 14 days
     *
     * To test immediately, change to: "0 * * * * *" (every minute)
     */
    @Scheduled(cron = "${app.menu.schedule}")
    public void sendBiWeeklyMealPlan() {
        log.info("⏰ Scheduled job triggered — generating bi-weekly meal plan");

        try {
            // Generate fresh plan
            Map<String, Object> plan = recipeService.generateMealPlan("balanced, healthy, variety");

            if (plan.containsKey("error")) {
                log.error("Failed to generate meal plan: {}", plan.get("error"));
                return;
            }

            // Send email
            boolean sent = emailService.sendMealPlanEmail(recipientEmail, plan);

            if (sent) {
                log.info("✅ Bi-weekly meal plan sent to: {}", recipientEmail);
            } else {
                log.error("❌ Failed to send bi-weekly meal plan to: {}", recipientEmail);
            }

        } catch (Exception e) {
            log.error("Error in scheduled meal plan job", e);
        }
    }
}
