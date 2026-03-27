package com.feex.mealplannersystem.service.exception;

public class CustomNotFoundException extends RuntimeException {
    public static final String CUSTOM_NOT_FOUND_EXCEPTION = "%s with id %s not found";

    public CustomNotFoundException(String notFoundItemName, String notFoundItemId) { super(String.format(CUSTOM_NOT_FOUND_EXCEPTION, notFoundItemName, notFoundItemId)); }
}
