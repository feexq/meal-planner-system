package com.feex.mealplannersystem.repository.entity.mealplan;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ═══════════════════════════════════════════════════════════════════
//  MealPlanRecordEntity  — збережений тижневий план
// ═══════════════════════════════════════════════════════════════════
@Entity
@Table(name = "meal_plan_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MealPlanRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "daily_calorie_target", nullable = false)
    private Integer dailyCalorieTarget;

    @Column(name = "weekly_calorie_target", nullable = false)
    private Integer weeklyCalorieTarget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MealPlanStatus status = MealPlanStatus.ACTIVE;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<MealPlanSlotEntity> slots = new HashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FoodLogEntity> foodLogs = new HashSet<>();

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum MealPlanStatus { ACTIVE, COMPLETED, CANCELLED }
}
