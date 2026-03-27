package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.category.CategoryResponse;
import com.feex.mealplannersystem.dto.category.CategorySummaryResponse;
import com.feex.mealplannersystem.dto.category.CreateCategoryRequest;
import com.feex.mealplannersystem.dto.category.UpdateCategoryRequest;
import com.feex.mealplannersystem.service.CategoryService;
import com.feex.mealplannersystem.service.mapper.CategoryMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper mapper;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllRoots() {
        return ResponseEntity.ok(
                categoryService.getAllRoots().stream()
                        .map(mapper::toResponse)
                        .toList()
        );
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategorySummaryResponse>> getAll() {
        return ResponseEntity.ok(
                categoryService.getAll().stream()
                        .map(mapper::toSummaryResponse)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(categoryService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(mapper.toResponse(categoryService.getBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoryRequest request
    ) {
        return ResponseEntity.ok(mapper.toResponse(categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
