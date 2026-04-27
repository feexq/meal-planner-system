package com.feex.mealplannersystem.service.oauth2;

import com.feex.mealplannersystem.config.jwt.JwtTokenProvider;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.security.oauth2.CustomOAuth2User;
import com.feex.mealplannersystem.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
        UserEntity user = oauthUser.getUser();

        String accessToken = tokenProvider.generateAccessToken(user);

        // Видаляємо старий refresh і створюємо новий — як при звичайному логіні
        refreshTokenService.deleteByUser(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        String targetUrl = "http://localhost:3000/oauth2/redirect"
                + "?token=" + accessToken
                + "&refreshToken=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
