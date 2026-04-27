package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagRequest;
import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagResponse;
import com.feex.mealplannersystem.service.mapper.TagMapper;
import com.feex.mealplannersystem.repository.TagRepository;
import com.feex.mealplannersystem.repository.entity.tag.TagEntity;
import com.feex.mealplannersystem.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    public IngredientTagResponse create(IngredientTagRequest request) {
        TagEntity entity = tagMapper.toEntity(request);

        return tagMapper.toResponse(
                tagRepository.save(entity)
        );
    }

    @Override
    public IngredientTagResponse update(Long id, IngredientTagRequest request) {
        TagEntity tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tag not found"));

        tag.setName(request.name());

        return tagMapper.toResponse(
                tagRepository.save(tag)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public IngredientTagResponse getById(Long id) {
        return tagMapper.toResponse(
                tagRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Tag not found"))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientTagResponse> getAll() {
        return tagRepository.findAll()
                .stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    public void delete(Long id) {
        tagRepository.deleteById(id);
    }
}