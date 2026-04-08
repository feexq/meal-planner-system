package com.feex.mealplannersystem.repository.entity.profile;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "weight_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_weight_history_user_date",
                        columnNames = {"user_id", "recorded_date"})
        },
        indexes = {
                @Index(name = "idx_weight_history_user_date",
                        columnList = "user_id, recorded_date DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;

    @Column(name = "note", length = 200)
    private String note;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}