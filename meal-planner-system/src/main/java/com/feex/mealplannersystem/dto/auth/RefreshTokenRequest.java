package com.feex.mealplannersystem.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class RefreshTokenRequest {
    String refreshToken;
}
