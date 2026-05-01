package com.feex.mealplannersystem.web;

import com.feex.mealplannersystem.dto.product.CreateProductRequest;
import com.feex.mealplannersystem.dto.product.ProductResponse;
import com.feex.mealplannersystem.dto.product.ProductSummaryResponse;
import com.feex.mealplannersystem.dto.product.UpdateProductRequest;
import com.feex.mealplannersystem.service.ProductService;
import com.feex.mealplannersystem.service.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for grocery store products")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper mapper;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) List<Long> categoryIds,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductSummaryResponse> response = productService
                .getAll(search, available, categoryIds, pageable)
                .map(mapper::toSummaryResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(productService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(mapper.toResponse(productService.getBySlug(slug)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateProductRequest request
    ) {
        return ResponseEntity.ok(mapper.toResponse(productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-ingredients")
    public ResponseEntity<List<ProductResponse>> getByIngredients(@RequestParam List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(productService.findAllByIngredientIds(ingredientIds).stream()
                .map(mapper::toResponse)
                .toList());
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String imageUrl = productService.uploadImage(id, file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        productService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
