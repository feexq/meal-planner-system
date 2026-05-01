package com.feex.mealplannersystem.util;

import com.feex.mealplannersystem.repository.entity.auth.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ResolveCartKeyHelper {
    public String resolveCartKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            UserEntity user = (UserEntity) auth.getPrincipal();
            return "cart:user:" + user.getId();
        }

        String sessionId = request.getHeader("X-Cart-Session");
        if (sessionId != null && !sessionId.isBlank()) {
            return "cart:session:" + sessionId;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Cart-Session header is required for guests");
    }
}
