package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findBySlug(String slug);
    boolean existsByName(String name);
    List<CategoryEntity> findAllByParentIsNull();  // всі кореневі
}
