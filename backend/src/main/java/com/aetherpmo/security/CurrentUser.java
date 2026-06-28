package com.aetherpmo.security;

import com.aetherpmo.common.ApiError;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    /**
     * Returns the authenticated user id, or null if unauthenticated.
     */
    public Long idOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long id)) {
            return null;
        }
        return id;
    }

    /**
     * Returns the authenticated user id, throwing if unauthenticated.
     */
    public Long id() {
        Long id = idOrNull();
        if (id == null) {
            throw ApiError.unauthorized("Not authenticated");
        }
        return id;
    }
}
