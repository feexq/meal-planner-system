package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.tag.base.BaseTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.base.BaseTagResponse;
import com.feex.mealplannersystem.dto.tag.base.BaseTagUpdateRequest;

import java.util.List;

public interface BaseTagService {
    List<BaseTagResponse> getAll();
    BaseTagResponse getById(Long id);
    BaseTagResponse getBySlug(String slug);
    BaseTagResponse create(BaseTagCreateRequest request);
    BaseTagResponse update(Long id, BaseTagUpdateRequest request);
    void delete(Long id);
}
