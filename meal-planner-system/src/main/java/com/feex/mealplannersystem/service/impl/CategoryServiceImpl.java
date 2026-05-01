package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.domain.category.Category;
import com.feex.mealplannersystem.dto.category.CreateCategoryRequest;
import com.feex.mealplannersystem.dto.category.UpdateCategoryRequest;
import com.feex.mealplannersystem.repository.CategoryRepository;
import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import com.feex.mealplannersystem.service.CategoryService;
import com.feex.mealplannersystem.service.ImageUploadService;
import com.feex.mealplannersystem.service.exception.CategoryParentException;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ImageUploadService imageUploadService;

    @Value("${spring.azure.storage.product_container}")
    private String productContainer;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllRoots() {
        return categoryRepository.findAllByParentIsNull().stream()
                .map(categoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toDomain)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Category getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(categoryMapper::toDomain)
                .orElseThrow(() -> new CustomNotFoundException("Category", slug));
    }

    @Override
    @Transactional
    public Category create(CreateCategoryRequest request) {
        if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
            throw new CustomAlreadyExistsException("Category", request.getSlug());
        }

        CategoryEntity parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomNotFoundException("Parent Category", request.getParentId().toString()));
        }

        CategoryEntity category = CategoryEntity.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .imageUrl(request.getImageUrl())
                .parent(parent)
                .build();

        return categoryMapper.toDomain(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public Category update(Long id, UpdateCategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));

        if (request.getName() != null) {
            category.setName(request.getName());
        }

        if (request.getSlug() != null && !request.getSlug().equals(category.getSlug())) {
            if (categoryRepository.findBySlug(request.getSlug()).isPresent()) {
                throw new CustomAlreadyExistsException("Category", request.getSlug());
            }
            category.setSlug(request.getSlug());
        }

        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new CategoryParentException("Category cannot be its own parent.");
            }
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomNotFoundException("Parent Category", request.getParentId().toString()));
            category.setParent(parent);
        }

        return categoryMapper.toDomain(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new CategoryParentException("Cannot delete category with active subcategories.");
        }

        if (category.getImageUrl() != null) {
            imageUploadService.deleteImage(category.getImageUrl(), productContainer);
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public String uploadImage(Long id, MultipartFile file) throws IOException {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));

        if (category.getImageUrl() != null) {
            imageUploadService.deleteImage(category.getImageUrl(), productContainer);
        }

        String imageUrl = imageUploadService.uploadImage(file, category.getSlug(), productContainer);

        category.setImageUrl(imageUrl);
        categoryRepository.save(category);

        return imageUrl;
    }

    @Transactional
    public void deleteImage(Long id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));
                
        if (category.getImageUrl() != null) {
            imageUploadService.deleteImage(category.getImageUrl(), productContainer);
            category.setImageUrl(null);
            categoryRepository.save(category);
        }
    }
}
