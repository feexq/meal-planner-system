package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.domain.category.Category;
import com.feex.mealplannersystem.dto.category.CreateCategoryRequest;
import com.feex.mealplannersystem.dto.category.UpdateCategoryRequest;
import com.feex.mealplannersystem.repository.CategoryRepository;
import com.feex.mealplannersystem.repository.entity.category.CategoryEntity;
import com.feex.mealplannersystem.service.CategoryService;
import com.feex.mealplannersystem.service.exception.CategoryParentException;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.CategoryMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllRoots() {
        return categoryRepository.findAllByParentIsNull().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAll() {
        return categoryRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Category getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(mapper::toDomain)
                .orElseThrow(() -> new CustomNotFoundException("Category", slug));
    }

    @Override
    @Transactional
    public Category create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new CustomAlreadyExistsException("Category", request.getName());
        }

        CategoryEntity entity = CategoryEntity.builder()
                .name(request.getName())
                .slug(toSlug(request.getName()))
                .build();

        if (request.getParentId() != null) {
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomNotFoundException("Parent category", request.getParentId().toString()));
            entity.setParent(parent);
        }

        return mapper.toDomain(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public Category update(Long id, UpdateCategoryRequest request) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Category", id.toString()));

        if (request.getName() != null) {
            entity.setName(request.getName());
            entity.setSlug(toSlug(request.getName()));
        }

        if (request.getParentId() != null) {
            CategoryEntity parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new CustomNotFoundException("Parent category", request.getParentId().toString()));

            if (parent.getId().equals(id)) {
                throw new CategoryParentException(parent.getId().toString());
            }
            entity.setParent(parent);
        }

        return mapper.toDomain(categoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CustomNotFoundException("Category", id.toString());
        }
        categoryRepository.deleteById(id);
    }

    private String toSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
