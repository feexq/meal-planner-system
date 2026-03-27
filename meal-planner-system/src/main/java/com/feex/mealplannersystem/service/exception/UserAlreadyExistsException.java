package com.feex.mealplannersystem.service.exception;

public class UserAlreadyExistsException extends RuntimeException{

    public static final String USER_ALREADY_EXISTS = "User with email %s, already exists";

    public UserAlreadyExistsException(String email) {super(String.format(USER_ALREADY_EXISTS, email));}
}
