package com.feex.mealplannersystem.repository.entity.profile;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "daily_nutrition_summary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_daily_summary_user_date",
                        columnNames = {"user_id", "summary_date"})
        },
        indexes = {
                @Index(name = "idx_daily_summary_user_date",
                        columnList = "user_id, summary_date DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyNutritionSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "total_calories")
    @Builder.Default
    private Double totalCalories = 0.0;

    @Column(name = "total_protein_g")
    @Builder.Default
    private Double totalProteinG = 0.0;

    @Column(name = "total_carbs_g")
    @Builder.Default
    private Double totalCarbsG = 0.0;

    @Column(name = "total_fat_g")
    @Builder.Default
    private Double totalFatG = 0.0;

    @Column(name = "planned_slots", nullable = false)
    @Builder.Default
    private Integer plannedSlots = 0;

    @Column(name = "eaten_slots", nullable = false)
    @Builder.Default
    private Integer eatenSlots = 0;

    @Column(name = "completion_rate", nullable = false)
    @Builder.Default
    private Integer completionRate = 0;

    @Column(name = "calorie_target")
    private Integer calorieTarget;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}