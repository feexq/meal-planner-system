package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.product.CreateProductRequest;
import com.feex.mealplannersystem.dto.product.UpdateProductRequest;
import com.feex.mealplannersystem.repository.CategoryRepository;
import com.feex.mealplannersystem.repository.IngTagRepository;
import com.feex.mealplannersystem.repository.ProductRepository;
import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import com.feex.mealplannersystem.repository.entity.product.ProductEntity;
import com.feex.mealplannersystem.repository.entity.tag.IngTagEntity;
import com.feex.mealplannersystem.service.ProductService;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final IngTagRepository tagRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductEntity> getAll(String search, Boolean available, List<Long> categoryIds, Pageable pageable) {
        return productRepository.findAllWithFilters(search, available, categoryIds, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductEntity getById(Long id) {
        return productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new CustomNotFoundException("Product", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductEntity getBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomNotFoundException("Product slug", slug));
    }

    @Override
    @Transactional
    public ProductEntity create(CreateProductRequest request) {
        if (productRepository.existsByNameUk(request.getNameUk())) {
            throw new CustomAlreadyExistsException("Product", request.getNameUk());
        }

        String slug = toSlug(request.getNameUk());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 1000;
        }

        ProductEntity entity = ProductEntity.builder()
                .nameUk(request.getNameUk())
                .slug(slug)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .price(request.getPrice())
                .unit(request.getUnit())
                .stock(request.getStock() != null ? request.getStock() : 0)
                .available(request.getIsAvailable() != null ? request.getIsAvailable() : false)
                .calories(request.getCalories())
                .proteinG(request.getProteinG())
                .fatG(request.getFatG())
                .carbsG(request.getCarbsG())
                .calorieConfidence(request.getCalorieConfidence())
                .build();

        updateCategoryAndTags(entity, request.getCategoryId(), request.getTagIds());

        return productRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductEntity> findAllByIngredientIds(List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) return List.of();
        return productRepository.findAllByIngredientIds(ingredientIds);
    }

    @Override
    @Transactional
    public ProductEntity update(Long id, UpdateProductRequest request) {
        ProductEntity entity = getById(id);

        if (request.getNameUk() != null && !request.getNameUk().equals(entity.getNameUk())) {
            if (productRepository.existsByNameUk(request.getNameUk())) {
                throw new CustomAlreadyExistsException("Product", request.getNameUk());
            }
            entity.setNameUk(request.getNameUk());
        }

        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getImageUrl() != null) entity.setImageUrl(request.getImageUrl());
        if (request.getPrice() != null) entity.setPrice(request.getPrice());
        if (request.getUnit() != null) entity.setUnit(request.getUnit());
        if (request.getStock() != null) entity.setStock(request.getStock());
        if (request.getIsAvailable() != null) entity.setAvailable(request.getIsAvailable());

        if (request.getCalories() != null) entity.setCalories(request.getCalories());
        if (request.getProteinG() != null) entity.setProteinG(request.getProteinG());
        if (request.getFatG() != null) entity.setFatG(request.getFatG());
        if (request.getCarbsG() != null) entity.setCarbsG(request.getCarbsG());
        if (request.getCalorieConfidence() != null) entity.setCalorieConfidence(request.getCalorieConfidence());

        updateCategoryAndTags(entity, request.getCategoryId(), request.getTagIds());

        return productRepository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new CustomNotFoundException("Product", id.toString());
        }
        productRepository.deleteById(id);
    }

    private void updateCategoryAndTags(ProductEntity entity, Long categoryId, Set<Long> tagIds) {
        if (categoryId != null) {
            CategoryEntity category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CustomNotFoundException("Category", categoryId.toString()));
            entity.setCategory(category);
        }

        if (tagIds != null) {
            Set<IngTagEntity> tags = new HashSet<>(tagRepository.findAllById(tagIds));
            entity.setTags(tags);
        }
    }

    private String toSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9а-яіїєґ]+", "-") // Додана підтримка кирилиці для слага
                .replaceAll("^-|-$", "");
    }
}