package com.feex.mealplannersystem.repository.entity;

import com.feex.mealplannersystem.common.DietaryConditionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dietary_conditions")
@Data
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