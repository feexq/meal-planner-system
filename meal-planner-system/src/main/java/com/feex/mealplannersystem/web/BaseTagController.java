package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagRequest;
import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagResponse;
import com.feex.mealplannersystem.dto.tag.base.BaseTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.base.BaseTagResponse;
import com.feex.mealplannersystem.dto.tag.base.BaseTagUpdateRequest;
import com.feex.mealplannersystem.service.BaseTagService;
import com.feex.mealplannersystem.service.IngTagService;
import com.feex.mealplannersystem.service.RecipeTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags-base")
@RequiredArgsConstructor
public class BaseTagController {

    private final BaseTagService baseTagService;

    @GetMapping()
    public ResponseEntity<List<BaseTagResponse>> getAllTags() {
        return ResponseEntity.ok(baseTagService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseTagResponse> getTagById(@PathVariable Long id) {
        return ResponseEntity.ok(baseTagService.getById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<BaseTagResponse> getTagBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(baseTagService.getBySlug(slug));
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseTagResponse> createTag(@Valid @RequestBody BaseTagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(baseTagService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseTagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody BaseTagUpdateRequest request) {
        return ResponseEntity.ok(baseTagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        baseTagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}