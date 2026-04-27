package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.tag.product.ProductTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.product.ProductTagResponse;
import com.feex.mealplannersystem.dto.tag.product.ProductTagUpdateRequest;
import com.feex.mealplannersystem.repository.IngTagRepository;
import com.feex.mealplannersystem.repository.entity.tag.IngTagEntity;
import com.feex.mealplannersystem.service.IngTagService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.TagMapper;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngTagServiceImpl implements IngTagService {

    private final IngTagRepository tagRepository;
    private final IngredientRepository ingredientRepository;
    private final TagMapper          tagMapper;

    @Transactional(readOnly = true)
    public List<ProductTagResponse> getAll() {
        return tagRepository.findAll()
                .stream()
                .map(tagMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductTagResponse getById(Long id) {
        return tagMapper.toDto(findTagOrThrow(id));
    }

    @Transactional(readOnly = true)
    public ProductTagResponse getBySlug(String slug) {
        return tagMapper.toDto(
                tagRepository.findBySlug(slug)
                        .orElseThrow(() -> new CustomNotFoundException("Tag", slug))
        );
    }

    @Transactional
    public ProductTagResponse create(ProductTagCreateRequest request) {
        if (tagRepository.existsBySlug(request.slug())) {
            throw new RuntimeException("Tag slug already exists: " + request.slug());
        }
        if (tagRepository.existsByName(request.name())) {
            throw new RuntimeException("Tag name already exists: " + request.name());
        }
        IngTagEntity saved = tagRepository.save(tagMapper.toEntity(request));
        return tagMapper.toDto(saved);
    }

    @Transactional
    public ProductTagResponse update(Long id, ProductTagUpdateRequest request) {
        IngTagEntity tag = findTagOrThrow(id);

        if (request.name() != null && !request.name().equals(tag.getName())) {
            if (tagRepository.existsByName(request.name())) {
                throw new RuntimeException("Tag name already exists: " + request.name());
            }
            tag.setName(request.name());
        }
        if (request.slug() != null && !request.slug().equals(tag.getSlug())) {
            if (tagRepository.existsBySlug(request.slug())) {
                throw new RuntimeException("Tag slug already exists: " + request.slug());
            }
            tag.setSlug(request.slug());
        }
        if (request.color() != null) {
            tag.setColor(request.color());
        }

        return tagMapper.toDto(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new CustomNotFoundException("Tag", id.toString());
        }
        tagRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductTagResponse> getTagsForIngredient(Long ingredientId) {
        findIngredientOrThrow(ingredientId);
        return tagRepository.findAllByIngredientId(ingredientId)
                .stream()
                .map(tagMapper::toDto)
                .toList();
    }

    @Transactional
    public void addTagToIngredient(Long ingredientId, Long tagId) {
        IngredientEntity ingredient = findIngredientOrThrow(ingredientId);
        IngTagEntity        tag        = findTagOrThrow(tagId);
        ingredient.getTags().add(tag);
        ingredientRepository.save(ingredient);
    }

    @Transactional
    public void removeTagFromIngredient(Long ingredientId, Long tagId) {
        IngredientEntity ingredient = findIngredientOrThrow(ingredientId);
        IngTagEntity        tag        = findTagOrThrow(tagId);
        ingredient.getTags().remove(tag);
        ingredientRepository.save(ingredient);
    }

    private IngTagEntity findTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found: " + id));
    }

    private IngredientEntity findIngredientOrThrow(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found: " + id));
    }
}