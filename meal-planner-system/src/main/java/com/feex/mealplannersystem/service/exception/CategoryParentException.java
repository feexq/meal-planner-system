package com.feex.mealplannersystem.service.exception;

public class CategoryParentException extends RuntimeException {
    public static final String CATEGORY_PARENT_EXCEPTION = "Category cannot be its own parent %s";

    public CategoryParentException(String categoryId) { super(String.format(CATEGORY_PARENT_EXCEPTION, categoryId)); }
}
