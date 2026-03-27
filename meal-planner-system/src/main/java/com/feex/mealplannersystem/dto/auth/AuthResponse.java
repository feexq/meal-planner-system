package com.feex.mealplannersystem.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String tokenType = "Bearer";
    Long expiresIn;
}
