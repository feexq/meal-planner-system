package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.dto.tag.base.BaseTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.base.BaseTagResponse;
import com.feex.mealplannersystem.dto.tag.base.BaseTagUpdateRequest;
import com.feex.mealplannersystem.repository.BaseTagRepository;
import com.feex.mealplannersystem.repository.entity.tag.BaseTagEntity;
import com.feex.mealplannersystem.service.BaseTagService;
import com.feex.mealplannersystem.service.exception.CustomAlreadyExistsException;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.BaseTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseTagServiceImpl implements BaseTagService {

    private final BaseTagRepository tagRepository;
    private final BaseTagMapper recipeTagMapper;

    @Transactional(readOnly = true)
    public List<BaseTagResponse> getAll() {
        return tagRepository.findAll()
                .stream()
                .map(recipeTagMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BaseTagResponse getById(Long id) {
        return recipeTagMapper.toDto(findTagOrThrow(id));
    }

    @Transactional(readOnly = true)
    public BaseTagResponse getBySlug(String slug) {
        return recipeTagMapper.toDto(
                tagRepository.findBySlug(slug)
                        .orElseThrow(() -> new CustomNotFoundException("Tag", slug))
        );
    }

    @Transactional
    public BaseTagResponse create(BaseTagCreateRequest request) {
        if (tagRepository.existsBySlug(request.slug())) {
            throw new CustomAlreadyExistsException("Tag" , request.slug());
        }
        if (tagRepository.existsByName(request.name())) {
            throw new CustomAlreadyExistsException("Tag" , request.name());
        }
        BaseTagEntity saved = tagRepository.save(recipeTagMapper.toEntity(request));
        return recipeTagMapper.toDto(saved);
    }

    @Transactional
    public BaseTagResponse update(Long id, BaseTagUpdateRequest request) {
        BaseTagEntity tag = findTagOrThrow(id);

        if (request.name() != null && !request.name().equals(tag.getName())) {
            if (tagRepository.existsByName(request.name())) {
                throw new CustomAlreadyExistsException("Tag" , request.name());
            }
            tag.setName(request.name());
        }
        if (request.slug() != null && !request.slug().equals(tag.getSlug())) {
            if (tagRepository.existsBySlug(request.slug())) {
                throw new CustomAlreadyExistsException("Tag" , request.slug());
            }
            tag.setSlug(request.slug());
        }
        if (request.color() != null) {
            tag.setColor(request.color());
        }

        return recipeTagMapper.toDto(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new CustomNotFoundException("Tag", id.toString());
        }
        tagRepository.deleteById(id);
    }

    private BaseTagEntity findTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new CustomNotFoundException("Tag", id.toString()));
    }
}
