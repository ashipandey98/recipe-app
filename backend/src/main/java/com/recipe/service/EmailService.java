package com.recipe.service;

import com.recipe.entity.MealPlan;
import com.recipe.repository.MealPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MealPlanRepository mealPlanRepository;

    /**
     * Send the 2-week meal plan to the user's email
     */
    public boolean sendMealPlanEmail(String toEmail, Map<String, Object> mealPlan) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🍽️ Your 2-Week Meal Plan — " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            helper.setText(buildEmailHtml(mealPlan), true); // true = HTML

            mailSender.send(message);

            // Mark as sent in DB
            mealPlanRepository.findTopByOrderByCreatedAtDesc().ifPresent(plan -> {
                plan.setEmailSent(true);
                plan.setRecipientEmail(toEmail);
                plan.setEmailSentAt(LocalDateTime.now());
                mealPlanRepository.save(plan);
            });

            log.info("Meal plan email sent to: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Failed to send email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Build the HTML email — beautiful, readable layout
     */
    @SuppressWarnings("unchecked")
    private String buildEmailHtml(Map<String, Object> plan) {
        StringBuilder html = new StringBuilder();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
              body { font-family: Georgia, serif; background: #f5f0e8; margin: 0; padding: 20px; color: #3d2b1f; }
              .container { max-width: 700px; margin: 0 auto; background: #fffdf7; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
              .header { background: #5c3d2e; color: #f5e6d3; padding: 40px 30px; text-align: center; }
              .header h1 { margin: 0; font-size: 2em; letter-spacing: 2px; }
              .header p { margin: 10px 0 0; opacity: 0.8; font-style: italic; }
              .week-header { background: #8b5e3c; color: white; padding: 15px 30px; font-size: 1.1em; font-weight: bold; letter-spacing: 1px; }
              .day-row { display: flex; border-bottom: 1px solid #e8ddd0; }
              .day-label { background: #f0e6d3; padding: 15px; width: 100px; font-weight: bold; color: #5c3d2e; font-size: 0.9em; display: flex; align-items: center; justify-content: center; text-align: center; flex-shrink: 0; }
              .meals { padding: 15px 20px; flex: 1; }
              .meal { margin: 4px 0; font-size: 0.9em; }
              .meal-icon { margin-right: 8px; }
              .shopping-section { padding: 30px; background: #faf6f0; }
              .shopping-section h2 { color: #5c3d2e; margin-top: 0; }
              .shopping-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
              .shopping-list { background: white; padding: 20px; border-radius: 8px; border: 1px solid #e8ddd0; }
              .shopping-list h3 { color: #8b5e3c; margin-top: 0; font-size: 1em; }
              .shopping-list ul { margin: 0; padding-left: 20px; }
              .shopping-list li { margin: 6px 0; font-size: 0.9em; }
              .tips-section { padding: 30px; }
              .tips-section h2 { color: #5c3d2e; margin-top: 0; }
              .tip { background: #f0e6d3; padding: 12px 16px; border-radius: 6px; margin: 8px 0; font-size: 0.9em; border-left: 3px solid #8b5e3c; }
              .footer { background: #5c3d2e; color: #f5e6d3; padding: 20px 30px; text-align: center; font-size: 0.85em; }
              .footer p { margin: 5px 0; opacity: 0.8; }
            </style>
            </head>
            <body>
            <div class="container">
              <div class="header">
                <h1>🍽️ Your 2-Week Meal Plan</h1>
                <p>Generated on """ + date + """
                </p>
              </div>
            """);

        // Week 1
        html.append(buildWeekHtml(plan, "week1", "📅 Week 1"));

        // Week 2
        html.append(buildWeekHtml(plan, "week2", "📅 Week 2"));

        // Shopping list
        if (plan.containsKey("shoppingList")) {
            Map<String, List<String>> shopping = (Map<String, List<String>>) plan.get("shoppingList");
            html.append("""
                <div class="shopping-section">
                  <h2>🛒 Shopping Lists</h2>
                  <div class="shopping-grid">
                """);

            if (shopping.containsKey("week1")) {
                html.append("<div class=\"shopping-list\"><h3>Week 1</h3><ul>");
                shopping.get("week1").forEach(item ->
                        html.append("<li>").append(item).append("</li>"));
                html.append("</ul></div>");
            }

            if (shopping.containsKey("week2")) {
                html.append("<div class=\"shopping-list\"><h3>Week 2</h3><ul>");
                shopping.get("week2").forEach(item ->
                        html.append("<li>").append(item).append("</li>"));
                html.append("</ul></div>");
            }

            html.append("</div></div>");
        }

        // Nutrition tips
        if (plan.containsKey("nutritionTips")) {
            List<String> tips = (List<String>) plan.get("nutritionTips");
            html.append("<div class=\"tips-section\"><h2>💡 Nutrition Tips</h2>");
            tips.forEach(tip -> html.append("<div class=\"tip\">").append(tip).append("</div>"));
            html.append("</div>");
        }

        // Footer
        html.append("""
              <div class="footer">
                <p>🍽️ Leftover Recipe Generator</p>
                <p>This plan auto-refreshes every 2 weeks. Enjoy your meals!</p>
              </div>
            </div>
            </body>
            </html>
            """);

        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String buildWeekHtml(Map<String, Object> plan, String weekKey, String weekLabel) {
        if (!plan.containsKey(weekKey)) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"week-header\">").append(weekLabel).append("</div>");

        List<Map<String, String>> days = (List<Map<String, String>>) plan.get(weekKey);
        for (Map<String, String> day : days) {
            sb.append("""
                    <div class="day-row">
                      <div class="day-label">""")
                    .append(day.getOrDefault("day", ""))
                    .append("""
                      </div>
                      <div class="meals">
                        <div class="meal"><span class="meal-icon">☀️</span>""")
                    .append(day.getOrDefault("breakfast", ""))
                    .append("""
                        </div>
                        <div class="meal"><span class="meal-icon">🌤️</span>""")
                    .append(day.getOrDefault("lunch", ""))
                    .append("""
                        </div>
                        <div class="meal"><span class="meal-icon">🌙</span>""")
                    .append(day.getOrDefault("dinner", ""))
                    .append("</div></div></div>");
        }

        return sb.toString();
    }
}
