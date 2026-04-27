package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.product.CreateProductRequest;
import com.feex.mealplannersystem.dto.product.UpdateProductRequest;
import com.feex.mealplannersystem.repository.entity.product.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Page<ProductEntity> getAll(String search, Boolean available, List<Long> categoryIds, Pageable pageable);

    ProductEntity getById(Long id);

    ProductEntity getBySlug(String slug);

    ProductEntity create(CreateProductRequest request);

    ProductEntity update(Long id, UpdateProductRequest request);

    void delete(Long id);

    List<ProductEntity> findAllByIngredientIds(List<Long> ingredientIds);
}