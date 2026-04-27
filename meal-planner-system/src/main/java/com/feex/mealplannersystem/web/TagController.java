package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagRequest;
import com.feex.mealplannersystem.dto.tag.ingredient.IngredientTagResponse;
import com.feex.mealplannersystem.dto.tag.product.ProductTagCreateRequest;
import com.feex.mealplannersystem.dto.tag.product.ProductTagResponse;
import com.feex.mealplannersystem.dto.tag.product.ProductTagUpdateRequest;
import com.feex.mealplannersystem.service.TagService;
import com.feex.mealplannersystem.service.impl.IngTagServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TagController {

    private final IngTagServiceImpl tagService;
    private final TagService tagRecipeService;

    @GetMapping("/tags")
    public ResponseEntity<List<ProductTagResponse>> getAllTags() {
        return ResponseEntity.ok(tagService.getAll());
    }

    @GetMapping("/tags/{id}")
    public ResponseEntity<ProductTagResponse> getTagById(@PathVariable Long id) {
        return ResponseEntity.ok(tagService.getById(id));
    }

    @GetMapping("/tags/slug/{slug}")
    public ResponseEntity<ProductTagResponse> getTagBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(tagService.getBySlug(slug));
    }

    @PostMapping("/tags")
    public ResponseEntity<ProductTagResponse> createTag(@Valid @RequestBody ProductTagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @PatchMapping("/tags/{id}")
    public ResponseEntity<ProductTagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody ProductTagUpdateRequest request) {
        return ResponseEntity.ok(tagService.update(id, request));
    }

    @DeleteMapping("/tags/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ingredients/{ingredientId}/tags")
    public ResponseEntity<List<ProductTagResponse>> getTagsForIngredient(
            @PathVariable Long ingredientId) {
        return ResponseEntity.ok(tagService.getTagsForIngredient(ingredientId));
    }

    @PostMapping("/ingredients/{ingredientId}/tags/{tagId}")
    public ResponseEntity<Void> addTagToIngredient(
            @PathVariable Long ingredientId,
            @PathVariable Long tagId) {
        tagService.addTagToIngredient(ingredientId, tagId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/ingredients/{ingredientId}/tags/{tagId}")
    public ResponseEntity<Void> removeTagFromIngredient(
            @PathVariable Long ingredientId,
            @PathVariable Long tagId) {
        tagService.removeTagFromIngredient(ingredientId, tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tags-recipes")
    public IngredientTagResponse create(
            @Valid @RequestBody IngredientTagRequest request
    ) {
        return tagRecipeService.create(request);
    }

    @PutMapping("/tags-recipes/{id}")
    public IngredientTagResponse update(
            @PathVariable Long id,
            @Valid @RequestBody IngredientTagRequest request
    ) {
        return tagRecipeService.update(id, request);
    }

    @GetMapping("/tags-recipes/{id}")
    public IngredientTagResponse getById(@PathVariable Long id) {
        return tagRecipeService.getById(id);
    }

    @GetMapping("/tags-recipes")
    public List<IngredientTagResponse> getAll() {
        return tagRecipeService.getAll();
    }

    @DeleteMapping("/tags-recipes/{id}")
    public void delete(@PathVariable Long id) {
        tagService.delete(id);
    }
}