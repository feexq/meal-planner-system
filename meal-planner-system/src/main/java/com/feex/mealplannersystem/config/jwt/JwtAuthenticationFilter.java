package com.feex.mealplannersystem.config.jwt;

import com.feex.mealplannersystem.service.CustomUserDetailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        System.out.println("=== shouldNotFilter: " + path);
        return path.equals("/api/auth/login") ||
                path.equals("/api/auth/register") ||
                path.equals("/api/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("=== doFilterInternal called for: " + request.getServletPath());
        System.out.println("=== Authorization header: " + request.getHeader("Authorization"));

        String token = getTokenFromRequest(request);
        System.out.println("=== Extracted token: " + token);

        if (StringUtils.hasText(token)) {
            System.out.println("=== JWT FILTER DEBUG ===");
            System.out.println("Authorization header: " + request.getHeader("Authorization"));
            System.out.println("Extracted token (first 50 chars): " + token.substring(0, Math.min(50, token.length())) + "...");

            if (jwtTokenProvider.validateToken(token)) {
                try {
                    String email = jwtTokenProvider.getEmailFromToken(token);
                    System.out.println("Token valid. Email from token: " + email);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute(HttpServletRequestWrapper.class.getName(), authentication);
                    System.out.println("✅ Authentication SUCCESSFULLY set for user: " + email);

                } catch (Exception e) {
                    System.out.println("❌ Error setting authentication: " + e.getMessage());
                }
            } else {
                System.out.println("❌ Token validation failed");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}