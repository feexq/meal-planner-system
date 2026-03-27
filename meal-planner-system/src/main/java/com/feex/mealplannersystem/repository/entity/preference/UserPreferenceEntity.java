package com.feex.mealplannersystem.repository.entity.preference;

import com.feex.mealplannersystem.common.survey.*;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private Integer age;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_intensity", nullable = false)
    private GoalIntensity goalIntensity;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", nullable = false)
    private DietType dietType;

    @Column(name = "meals_per_day", nullable = false)
    private Integer mealsPerDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_complexity")
    private CookComplexity cookingComplexity;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_level", nullable = false)
    private CookBudget budgetLevel;

    @Column(nullable = false)
    private Boolean zigzag;

    // === Нові OneToMany зв'язки ===
    @OneToMany(mappedBy = "userPreference",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserHealthConditionEntity> healthConditions = new ArrayList<>();

    @OneToMany(mappedBy = "userPreference",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserAllergyEntity> allergies = new ArrayList<>();

    @OneToMany(mappedBy = "userPreference",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserDislikedIngredientEntity> dislikedIngredients = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}