package com.feex.mealplannersystem.repository;

import com.feex.mealplannersystem.repository.entity.tag.BaseTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BaseTagRepository extends JpaRepository<BaseTagEntity, Long> {

    Optional<BaseTagEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    @Query("SELECT t FROM BaseTagEntity t JOIN t.ingredients i WHERE i.id = :ingredientId")
    List<BaseTagEntity> findAllByIngredientId(Long ingredientId);
}