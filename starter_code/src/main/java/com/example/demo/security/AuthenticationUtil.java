package com.example.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticationUtil {
    
    /**
     * Helper method for authorization check.
     * The logged-in user is allowed to execute the request only if:
     * 1. Their username (authentication.getName()) matches the target username (targetUsername), OR
     * 2. They possess the "ROLE_ADMIN" role.
     *
     * @param targetUsername The username whose cart is to be modified (from the request).
     * @param authentication The current authentication object of the logged-in user.
     * @return true if the user is authorized, false otherwise.
     */
    public static final boolean isAuthorized(String targetUsername, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 1. Check if the authenticated user is the target user
        if (authentication.getName().equals(targetUsername)) {
            return true;
        }

        // 2. Check if the authenticated user has the ROLE_ADMIN role
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

}
