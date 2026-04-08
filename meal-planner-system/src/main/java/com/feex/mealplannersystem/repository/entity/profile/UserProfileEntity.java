package com.feex.mealplannersystem.repository.entity.profile;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "timezone", nullable = false, length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "bio", length = 300)
    private String bio;

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