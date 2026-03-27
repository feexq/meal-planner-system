package com.feex.mealplannersystem.service.exception;

public class RefreshTokenExpiredException extends RuntimeException {
    public static final String REFRESH_TOKEN_EXPIRED_EXCEPTION = "Refresh token expired %s. Please login again.";

    public RefreshTokenExpiredException(String refreshToken) { super(String.format(REFRESH_TOKEN_EXPIRED_EXCEPTION, refreshToken)); }
}

