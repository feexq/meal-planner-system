package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.tag.product.ProductTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.product.ProductTagResponse;
import com.feex.mealplannersystem.dto.tag.product.ProductTagUpdateRequest;

import java.util.List;

public interface IngTagService {
    List<ProductTagResponse> getAll();

    ProductTagResponse getById(Long id);

    ProductTagResponse getBySlug(String slug);

    ProductTagResponse create(ProductTagCreateRequest request);

    ProductTagResponse update(Long id, ProductTagUpdateRequest request);

    void delete(Long id);

    List<ProductTagResponse> getTagsForIngredient(Long ingredientId);

    void addTagToIngredient(Long ingredientId, Long tagId);

    void removeTagFromIngredient(Long ingredientId, Long tagId);
}
