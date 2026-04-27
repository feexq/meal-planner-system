package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.mealplan.DietaryConditionType;
import com.feex.mealplannersystem.common.mealplan.DietaryTagStatus;
import com.feex.mealplannersystem.dto.dietary.DietaryConditionResponse;
import com.feex.mealplannersystem.dto.dietary.IngredientDietaryTagResponse;
import com.feex.mealplannersystem.dto.dietary.UpdateDietaryTagsRequest;
import com.feex.mealplannersystem.repository.DietaryConditionRepository;
import com.feex.mealplannersystem.repository.IngredientDietaryTagRepository;
import com.feex.mealplannersystem.repository.IngredientRepository;
import com.feex.mealplannersystem.repository.entity.DietaryConditionEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientDietaryTagEntity;
import com.feex.mealplannersystem.repository.entity.ingredient.IngredientEntity;
import com.feex.mealplannersystem.repository.other.IngredientDietaryTagId;
import com.feex.mealplannersystem.service.DietaryTagService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.mapper.DietaryConditionMapper;
import com.feex.mealplannersystem.service.mapper.DietaryTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DietaryTagServiceImpl implements DietaryTagService {

    private final DietaryConditionRepository conditionRepository;
    private final IngredientDietaryTagRepository tagRepository;
    private final IngredientRepository ingredientRepository;
    private final DietaryConditionMapper conditionMapper;
    private final DietaryTagMapper tagMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DietaryConditionResponse> getAllConditions() {
        return conditionRepository.findAll().stream()
                .map(conditionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DietaryConditionResponse> getConditionsByType(DietaryConditionType type) {
        return conditionRepository.findAllByType(type).stream()
                .map(conditionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientDietaryTagResponse> getTagsByIngredient(Long productId) {
        IngredientEntity ing = ingredientRepository.findFirstIngredientEntityByProduct_Id(productId);
        return tagRepository.findAllByIngredientId(ing.getId()).stream()
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void updateTags(Long ingredientId, UpdateDietaryTagsRequest request) {
        IngredientEntity ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new CustomNotFoundException("Ingredient", ingredientId.toString()));

        tagRepository.deleteAllByIngredientId(ingredientId);
        tagRepository.flush();

        List<String> conditionIds = new ArrayList<>(request.getTags().keySet());
        Map<String, DietaryConditionEntity> conditionsMap = conditionRepository.findAllById(conditionIds)
                .stream()
                .collect(Collectors.toMap(DietaryConditionEntity::getId, c -> c));

        List<IngredientDietaryTagEntity> tags = request.getTags().entrySet().stream()
                .map(entry -> {
                    DietaryConditionEntity condition = conditionsMap.get(entry.getKey());
                    if (condition == null) throw new CustomNotFoundException("Condition", entry.getKey());
                    return IngredientDietaryTagEntity.builder()
                            .id(new IngredientDietaryTagId(ingredientId, entry.getKey()))
                            .ingredient(ingredient)
                            .condition(condition)
                            .status(entry.getValue())
                            .build();
                })
                .toList();

        request.getTags().forEach((conditionId, status) ->
                tagRepository.insertTag(ingredientId, conditionId, status.name())
        );
    }

    @Override
    @Transactional
    public void saveBatchTags(Long ingredientId, Map<String, String> tags) {
        Map<String, DietaryTagStatus> converted = tags.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> DietaryTagStatus.valueOf(e.getValue().toUpperCase())
                ));

        updateTags(ingredientId, new UpdateDietaryTagsRequest(converted));
    }
}
