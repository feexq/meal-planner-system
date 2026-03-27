package com.feex.mealplannersystem.service.exception;

public class CustomAlreadyExistsException extends RuntimeException {
    public static final String ALREADY_EXISTS_EXCEPTION = "%s with id %s already exists";

    public CustomAlreadyExistsException(String existsItemName, String existsItemId) { super(String.format(ALREADY_EXISTS_EXCEPTION, existsItemName, existsItemId)); }
}
