package com.feex.mealplannersystem.domain.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    private Long id;
    private String name;
    private String slug;
    private String imageUrl;
    private Long parentId;
    private String parentName;
    private List<Category> children;
}
