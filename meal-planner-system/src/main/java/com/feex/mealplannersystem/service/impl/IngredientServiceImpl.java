package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.config.normalizer.IngredientNormalizationQueue;
import com.feex.mealplannersystem.config.normalizer.NormalizerClient;
import com.feex.mealplannersystem.domain.ingredient.Ingredient;
import com.feex.mealplannersystem.dto.ingredient.CreateIngredientRequest;
import com.feex.mealplannersystem.dto.ingredient.UpdateIngredientRequest;
import com.feex.mealplannersystem.repository.CategoryRepository;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientAliasEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.service.IngredientService;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.IngredientMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final CategoryRepository categoryRepository;
    private final NormalizerClient normalizerClient;
    private final IngredientMapper mapper;
    private final IngredientNormalizationQueue normalizationQueue;

    @Override
    @Transactional(readOnly = true)
    public Page<Ingredient> getAll(String search, Boolean available, Long categoryId, Pageable pageable) {
        List<Long> categoryIds = null;

        if (categoryId != null) {
            categoryIds = categoryRepository.findById(categoryId)
                    .map(cat -> {
                        List<Long> ids = new ArrayList<>();
                        ids.add(cat.getId());
                        cat.getChildren().forEach(child -> ids.add(child.getId()));
                        return ids;
                    })
                    .orElseThrow(() -> new CustomNotFoundException("Category", categoryId.toString()));
        }

        return ingredientRepository.findAllWithFilters(search, available, categoryIds, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Ingredient getById(Long id) {
        return ingredientRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new CustomNotFoundException("Ingredient", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Ingredient getBySlug(String slug) {
        return ingredientRepository.findBySlug(slug)
                .map(mapper::toDomain)
                .orElseThrow(() -> new CustomNotFoundException("Ingredient", slug));
    }

    @Override
    @Transactional
    public Ingredient create(CreateIngredientRequest request) {
        if (ingredientRepository.existsByNormalizedName(request.getNormalizedName())) {
            throw new CustomAlreadyExistsException("Ingredient", request.getNormalizedName());
        }

        String normalized = normalizerClient.normalize(request.getNormalizedName());

        IngredientEntity entity = IngredientEntity.builder()
                .normalizedName(normalized)
                .slug(toSlug(normalized))
                .imageUrl(request.getImageUrl())
                .available(request.isAvailable())
                .price(request.getPrice())
                .unit(request.getUnit())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .aliases(new HashSet<>())
                .build();

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomNotFoundException("Category", request.getCategoryId().toString()));
            entity.setCategory(category);
        }

        if (request.getAliases() != null) {
            request.getAliases().stream()
                    .map(raw -> IngredientAliasEntity.builder()
                            .rawName(raw)
                            .ingredient(entity)
                            .build())
                    .forEach(entity.getAliases()::add);
        }

        Ingredient saved = mapper.toDomain(ingredientRepository.save(entity));

        normalizationQueue.add(saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public Ingredient update(Long id, UpdateIngredientRequest request) {
        IngredientEntity entity = ingredientRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Ingredient", id.toString()));

        if (request.getNormalizedName() != null) {
            entity.setNormalizedName(request.getNormalizedName());
            entity.setSlug(toSlug(request.getNormalizedName()));
        }
        if (request.getImageUrl() != null) entity.setImageUrl(request.getImageUrl());

        if (request.getAvailable() != null) entity.setAvailable(request.getAvailable());

        if (request.getPrice() != null) entity.setPrice(request.getPrice());

        if (request.getUnit() != null) entity.setUnit(request.getUnit());

        if (request.getStock() != null) entity.setStock(request.getStock());

        if (request.getCategoryId() != null) {
            CategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomNotFoundException("Category", request.getCategoryId().toString()));
            entity.setCategory(category);
        }

        if (request.getAliases() != null) {
            entity.getAliases().clear();
            ingredientRepository.saveAndFlush(entity);

            request.getAliases().stream()
                    .map(raw -> IngredientAliasEntity.builder()
                            .rawName(raw)
                            .ingredient(entity)
                            .build())
                    .forEach(entity.getAliases()::add);
        }

        return mapper.toDomain(ingredientRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!ingredientRepository.existsById(id)) {
            throw new CustomNotFoundException("Ingredient", id.toString());
        }
        ingredientRepository.deleteById(id);
    }

    private String toSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}