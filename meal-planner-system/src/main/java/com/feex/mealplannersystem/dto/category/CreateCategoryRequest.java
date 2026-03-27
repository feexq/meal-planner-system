package com.feex.mealplannersystem.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    private String name;

    private Long parentId;  // null = коренева категорія
}
