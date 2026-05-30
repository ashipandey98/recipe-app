package com.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recipes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String ingredients;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private String cookingTime;

    private String cuisine;

    private String calories;

    @Column(columnDefinition = "TEXT")
    private String inputIngredients; // what user typed

    private boolean saved;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
