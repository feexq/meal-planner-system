package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagRequest;
import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagResponse;
import com.feex.mealplannersystem.service.mapper.RecipeTagMapper;
import com.feex.mealplannersystem.repository.TagRepository;
import com.feex.mealplannersystem.repository.entity.tag.RecipeTagEntity;
import com.feex.mealplannersystem.service.RecipeTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecipeTagServiceImpl implements RecipeTagService {

    private final TagRepository tagRepository;
    private final RecipeTagMapper recipeTagMapper;

    @Override
    public RecipeTagResponse create(RecipeTagRequest request) {
        RecipeTagEntity entity = recipeTagMapper.toEntity(request);

        return recipeTagMapper.toResponse(
                tagRepository.save(entity)
        );
    }

    @Override
    public RecipeTagResponse update(Long id, RecipeTagRequest request) {
        RecipeTagEntity tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        tag.setName(request.name());

        return recipeTagMapper.toResponse(
                tagRepository.save(tag)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeTagResponse getById(Long id) {
        return recipeTagMapper.toResponse(
                tagRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Tag not found"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeTagResponse> getAll() {
        return tagRepository.findAll()
                .stream()
                .map(recipeTagMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        tagRepository.deleteById(id);
    }
}