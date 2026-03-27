package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.common.DietaryConditionType;
import com.feex.mealplannersystem.config.normalizer.ClassificationScheduler;
import com.feex.mealplannersystem.dto.dietary.DietaryConditionResponse;
import com.feex.mealplannersystem.dto.dietary.IngredientDietaryTagResponse;
import com.feex.mealplannersystem.dto.dietary.UpdateDietaryTagsRequest;
import com.feex.mealplannersystem.service.DietaryTagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dietary")
public class DietaryTagController {

    private final DietaryTagService dietaryTagService;
    private final ClassificationScheduler classificationScheduler;

    @GetMapping("/api/dietary/conditions")
    public ResponseEntity<List<DietaryConditionResponse>> getAllConditions() {
        return ResponseEntity.ok(dietaryTagService.getAllConditions());
    }

    @GetMapping("/api/dietary/conditions/type/{type}")
    public ResponseEntity<List<DietaryConditionResponse>> getByType(
            @PathVariable DietaryConditionType type
    ) {
        return ResponseEntity.ok(dietaryTagService.getConditionsByType(type));
    }

    @PostMapping("/api/dietary-conditions/trigger-classification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> triggerClassification() {
        classificationScheduler.processBatch();
        return ResponseEntity.ok().build();
    }
}