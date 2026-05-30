package com.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String planContent; // full JSON plan from Gemini

    @Column(columnDefinition = "TEXT")
    private String shoppingList;

    private String recipientEmail;

    private boolean emailSent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
