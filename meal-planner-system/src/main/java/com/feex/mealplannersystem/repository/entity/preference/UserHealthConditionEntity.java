package com.feex.mealplannersystem.repository.entity.preference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_health_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"userPreference", "conditionName"})
public class UserHealthConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_preference_id", nullable = false)
    private UserPreferenceEntity userPreference;

    @Column(name = "condition_name", nullable = false, length = 255)
    private String conditionName;
}
