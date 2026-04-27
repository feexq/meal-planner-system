package com.feex.mealplannersystem.repository.entity;

import com.feex.mealplannersystem.common.mealplan.DietaryConditionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dietary_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DietaryConditionEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DietaryConditionType type;
}