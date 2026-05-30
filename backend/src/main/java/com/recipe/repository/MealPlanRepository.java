package com.recipe.repository;

import com.recipe.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Optional<MealPlan> findTopByOrderByCreatedAtDesc();
}
