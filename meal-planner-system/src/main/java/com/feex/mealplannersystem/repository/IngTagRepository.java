package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.tag.IngTagEntity;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngTagRepository extends JpaRepository<IngTagEntity, Long> {

    Optional<IngTagEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    @Query("SELECT t FROM IngTagEntity t JOIN t.ingredients i WHERE i.id = :ingredientId")
    List<IngTagEntity> findAllByIngredientId(Long ingredientId);
}