package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.dietary.IngredientDietaryTagResponse;
import com.feex.mealplannersystem.dto.dietary.UpdateDietaryTagsRequest;
import com.feex.mealplannersystem.dto.ingredient.CreateIngredientRequest;
import com.feex.mealplannersystem.dto.ingredient.IngredientResponse;
import com.feex.mealplannersystem.dto.ingredient.IngredientSummaryResponse;
import com.feex.mealplannersystem.dto.ingredient.UpdateIngredientRequest;
import com.feex.mealplannersystem.service.DietaryTagService;
import com.feex.mealplannersystem.service.IngredientService;
import com.feex.mealplannersystem.service.mapper.IngredientMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
@RequiredArgsConstructor
@Tag(name = "Ingredients")
public class IngredientController {

    private final IngredientService ingredientService;
    private final IngredientMapper mapper;
    private final DietaryTagService dietaryTagService;

    @GetMapping
    public ResponseEntity<Page<IngredientSummaryResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "normalizedName") Pageable pageable
    ) {
        return ResponseEntity.ok(
                ingredientService.getAll(search, available, categoryId, pageable)
                        .map(mapper::toSummaryResponse)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(ingredientService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<IngredientResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(mapper.toResponse(ingredientService.getBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IngredientResponse> create(@RequestBody @Valid CreateIngredientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(ingredientService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IngredientResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateIngredientRequest request
    ) {
        return ResponseEntity.ok(mapper.toResponse(ingredientService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("{id}/dietary-tags")
    public ResponseEntity<List<IngredientDietaryTagResponse>> getTagsByIngredient(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(dietaryTagService.getTagsByIngredient(id));
    }

    @PutMapping("{id}/dietary-tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateTags(
            @PathVariable Long id,
            @RequestBody UpdateDietaryTagsRequest request
    ) {
        dietaryTagService.updateTags(id, request);
        return ResponseEntity.noContent().build();
    }
}