package com.envforge.controlapi.security;

import com.envforge.controlapi.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * CurrentUserProvider for local development and tests (no real
 * authentication - see SecurityConfig). Active whenever the "entra"
 * profile is NOT set. Resolves the current user from debug headers
 * instead of a validated JWT.
 *
 * See EntraIdCurrentUserProvider for the real, JWT-backed implementation
 * used when running with --spring.profiles.active=entra (Ziua 31). Callers
 * (AuditService, AuthorizationService) do not need to change, since they
 * only depend on the CurrentUserProvider interface.
 */
@Component
@Profile("!entra")
public class DevCurrentUserProvider implements CurrentUserProvider {

    private static final String DEBUG_USER_EMAIL_HEADER = "X-Debug-User-Email";
    private static final String DEBUG_USER_NAME_HEADER = "X-Debug-User-Name";
    private static final String DEFAULT_EMAIL = "dev@envforge.local";

    private final HttpServletRequest request;

    public DevCurrentUserProvider(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public CurrentUser getCurrentUser() {
        String email = request.getHeader(DEBUG_USER_EMAIL_HEADER);
        if (email == null || email.isBlank()) {
            email = DEFAULT_EMAIL;
        }
        String displayName = request.getHeader(DEBUG_USER_NAME_HEADER);
        if (displayName == null || displayName.isBlank()) {
            displayName = email;
        }
        return new CurrentUser(email, email, displayName, Role.ADMIN);
    }
}
