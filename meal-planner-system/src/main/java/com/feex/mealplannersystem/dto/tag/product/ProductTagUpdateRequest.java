package com.feex.mealplannersystem.dto.tag.product;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductTagUpdateRequest(

        @Size(max = 100)
        String name,

        @Size(max = 100)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "slug must be lowercase kebab-case")
        String slug,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
                message = "color must be a valid HEX value like #6C3FC5")
        String color
) {}