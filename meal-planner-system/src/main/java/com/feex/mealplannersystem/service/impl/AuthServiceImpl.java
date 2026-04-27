package com.feex.mealplannersystem.service.impl;

import com.feex.mealplannersystem.common.user.Role;
import com.feex.mealplannersystem.config.jwt.JwtTokenProvider;
import com.feex.mealplannersystem.dto.auth.AuthResponse;
import com.feex.mealplannersystem.dto.auth.LoginRequest;
import com.feex.mealplannersystem.dto.auth.RegisterRequest;
import com.feex.mealplannersystem.repository.UserRepository;
import com.feex.mealplannersystem.repository.entity.auth.RefreshTokenEntity;
import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import com.feex.mealplannersystem.service.AuthService;
import com.feex.mealplannersystem.service.CartService;
import com.feex.mealplannersystem.service.RefreshTokenService;
import com.feex.mealplannersystem.service.exception.CustomNotFoundException;
import com.feex.mealplannersystem.service.exception.UserAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final CartService cartService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.USER)
                .enabled(true)
                .build();

        UserEntity savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(savedUser);
        RefreshTokenEntity refreshTokenEntity = refreshTokenService.createRefreshToken(savedUser);
        String refreshToken = refreshTokenEntity.getToken();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900L) // 15 хв
                .build();
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserEntity user = (UserEntity) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        refreshTokenService.deleteByUser(user);
        RefreshTokenEntity refreshTokenEntity = refreshTokenService.createRefreshToken(user);

        String sessionId = httpRequest.getHeader("X-Cart-Session");
        if (sessionId != null && !sessionId.isBlank()) {
            cartService.mergeCarts("cart:session:" + sessionId, "cart:user:" + user.getId());
        }

        System.out.println("X-Cart-Session header: " + sessionId); // ← має бути uuid, не null
        if (sessionId != null && !sessionId.isBlank()) {
            cartService.mergeCarts("cart:session:" + sessionId, "cart:user:" + user.getId());
        }

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .expiresIn(900L)
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshTokenEntity refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new CustomNotFoundException("Refresh token", refreshTokenStr));

        refreshTokenService.verifyExpiration(refreshToken);

        UserEntity user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        refreshTokenService.deleteByUser(user);
        RefreshTokenEntity newRefreshTokenEntity = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenEntity.getToken())
                .expiresIn(900L)
                .build();
    }

    @Transactional
    public void logout(String accessToken) {

    }
}
