package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.tag.base.BaseTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.base.BaseTagResponse;
import com.feex.mealplannersystem.dto.tag.base.BaseTagUpdateRequest;
import com.feex.mealplannersystem.repository.BaseTagRepository;
import com.feex.mealplannersystem.repository.entity.tag.BaseTagEntity;
import com.feex.mealplannersystem.service.IngTagService;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.BaseTagMapper;
import com.feex.mealplannersystem.service.mapper.RecipeTagMapper;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngTagServiceImpl implements IngTagService {

    private final BaseTagRepository tagRepository;
    private final IngredientRepository ingredientRepository;
    private final BaseTagMapper recipeTagMapper;

    @Transactional(readOnly = true)
    public List<BaseTagResponse> getTagsForIngredient(Long ingredientId) {
        findIngredientOrThrow(ingredientId);
        return tagRepository.findAllByIngredientId(ingredientId)
                .stream()
                .map(recipeTagMapper::toDto)
                .toList();
    }

    @Transactional
    public void addTagToIngredient(Long ingredientId, Long tagId) {
        IngredientEntity ingredient = findIngredientOrThrow(ingredientId);
        BaseTagEntity tag        = findTagOrThrow(tagId);
        ingredient.getTags().add(tag);
        ingredientRepository.save(ingredient);
    }

    @Transactional
    public void removeTagFromIngredient(Long ingredientId, Long tagId) {
        IngredientEntity ingredient = findIngredientOrThrow(ingredientId);
        BaseTagEntity tag        = findTagOrThrow(tagId);
        ingredient.getTags().remove(tag);
        ingredientRepository.save(ingredient);
    }

    private BaseTagEntity findTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Tag", id.toString()));
    }

    private IngredientEntity findIngredientOrThrow(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Ingredient", id.toString()));
    }
}