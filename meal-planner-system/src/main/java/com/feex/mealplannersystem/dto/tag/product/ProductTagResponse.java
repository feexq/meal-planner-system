package com.feex.mealplannersystem.dto.tag.product;

public record ProductTagResponse(
        Long   id,
        String name,
        String slug,
        String color
) {}