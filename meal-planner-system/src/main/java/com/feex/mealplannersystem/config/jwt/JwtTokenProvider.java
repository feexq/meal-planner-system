package com.feex.mealplannersystem.config.jwt;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(UserEntity user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(UserEntity user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            System.out.println("=== VALIDATING TOKEN ===");
            System.out.println("Token (first 50): " + token.substring(0, 50) + "...");

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);

            System.out.println("Token validation SUCCESS");
            return true;
        } catch (ExpiredJwtException ex) {
            System.out.println("Token expired");
            return false;
        } catch (UnsupportedJwtException ex) {
            System.out.println("Unsupported JWT");
            return false;
        } catch (MalformedJwtException ex) {
            System.out.println("Malformed JWT");
            return false;
        } catch (SignatureException ex) {
            System.out.println("Invalid signature");
            return false;
        } catch (Exception ex) {
            System.out.println("Token validation FAILED: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            return false;
        }
    }
}