package com.feex.mealplannersystem.repository.entity.profile;

import com.feex.mealplannersystem.common.user.StreakType;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "user_streak_meta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStreakMetaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "current_streak", nullable = false)
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak", nullable = false)
    @Builder.Default
    private Integer longestStreak = 0;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "streak_type", nullable = false)
    @Builder.Default
    private StreakType streakType = StreakType.CASUAL;

    @Column(name = "freezes_available", nullable = false)
    @Builder.Default
    private Integer freezesAvailable = 2;

    @Column(name = "freezes_used_this_month", nullable = false)
    @Builder.Default
    private Integer freezesUsedThisMonth = 0;

    @Column(name = "last_freeze_used_date")
    private LocalDate lastFreezeUsedDate;

    @Column(name = "total_active_days", nullable = false)
    @Builder.Default
    private Integer totalActiveDays = 0;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}