package com.feex.mealplannersystem.repository.entity.mealplan;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plan_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MealPlanSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MealPlanRecordEntity plan;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "meal_type", nullable = false)
    private String mealType;

    @Column(name = "slot_role", nullable = false)
    @Builder.Default
    private String slotRole = "main";

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "recipe_name")
    private String recipeName;

    @Column(name = "target_calories", nullable = false)
    private Double targetCalories;

    @Column(name = "actual_calories")
    private Double actualCalories;

    @Column(name = "protein_g")
    private Double proteinG;

    @Column(name = "carbs_g")
    private Double carbsG;

    @Column(name = "fat_g")
    private Double fatG;

    @Column(name = "recommended_servings")
    private Double recommendedServings;

    @Column(name = "portion_note", length = 500)
    private String portionNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SlotStatus status = SlotStatus.PLANNED;

    @Column(name = "eaten_at")
    private LocalDateTime eatenAt;

    public enum SlotStatus { PLANNED, EATEN, SKIPPED, REPLACED }
}


