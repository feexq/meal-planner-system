package com.feex.mealplannersystem.repository.entity.mealplan;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "food_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MealPlanRecordEntity plan;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "raw_input", nullable = false)
    private String rawInput;

    @Column(name = "parsed_items", columnDefinition = "TEXT")
    private String parsedItemsJson;

    @Column(name = "total_calories", nullable = false)
    private Double totalCalories;

    @Column(name = "total_protein_g")
    private Double totalProteinG;

    @Column(name = "total_carbs_g")
    private Double totalCarbsG;

    @Column(name = "total_fat_g")
    private Double totalFatG;

    @Column(name = "confidence")
    private String confidence;

    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }
}
