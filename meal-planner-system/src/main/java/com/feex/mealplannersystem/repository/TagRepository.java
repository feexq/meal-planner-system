package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.tag.RecipeTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<RecipeTagEntity, Long> {
    Optional<RecipeTagEntity> findByName(String name);
}
