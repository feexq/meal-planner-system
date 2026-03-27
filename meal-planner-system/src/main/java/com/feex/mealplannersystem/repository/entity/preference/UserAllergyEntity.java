package com.feex.mealplannersystem.repository.entity.preference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_allergies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"userPreference", "allergyName"})
public class UserAllergyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_preference_id", nullable = false)
    private UserPreferenceEntity userPreference;

    @Column(name = "allergy_name", nullable = false, length = 255)
    private String allergyName;
}
