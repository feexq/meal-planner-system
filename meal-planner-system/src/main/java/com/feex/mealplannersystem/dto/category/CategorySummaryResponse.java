package com.feex.mealplannersystem.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
}
