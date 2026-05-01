package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagRequest;
import com.feex.mealplannersystem.dto.tag.recipe.RecipeTagResponse;
import com.feex.mealplannersystem.service.RecipeTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags-recipes")
@RequiredArgsConstructor
public class RecipeTagController {

    private final RecipeTagService tagRecipeService;

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public RecipeTagResponse create(
            @Valid @RequestBody RecipeTagRequest request
    ) {
        return tagRecipeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RecipeTagResponse update(
            @PathVariable Long id,
            @Valid @RequestBody RecipeTagRequest request
    ) {
        return tagRecipeService.update(id, request);
    }

    @GetMapping("/{id}")
    public RecipeTagResponse getById(@PathVariable Long id) {
        return tagRecipeService.getById(id);
    }

    @GetMapping()
    public List<RecipeTagResponse> getAll() {
        return tagRecipeService.getAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        tagRecipeService.delete(id);
    }
}
