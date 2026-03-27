package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.auth.AuthResponse;
import com.feex.mealplannersystem.dto.auth.LoginRequest;
import com.feex.mealplannersystem.dto.auth.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    AuthResponse refreshToken(String refreshTokenStr);
    void logout(String accessToken);
}
