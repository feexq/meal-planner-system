package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.common.mealplan.DietaryConditionType;
import com.feex.mealplannersystem.repository.entity.DietaryConditionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DietaryConditionRepository extends JpaRepository<DietaryConditionEntity, String> {
    List<DietaryConditionEntity> findAllByType(DietaryConditionType type);
}
