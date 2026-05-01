package com.feex.mealplannersystem.dto.tag.base;

public record BaseTagResponse(
        Long   id,
        String name,
        String slug,
        String color
) {}