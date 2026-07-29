package com.envforge.controlapi.security;

import com.envforge.controlapi.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Temporary CurrentUserProvider for the permissive development phase.
 *
 * There is no real authentication yet (see SecurityConfig), so the
 * current user is resolved from debug headers instead of a validated
 * JWT. Replace this implementation in Săptămâna 7 once Entra ID / a JWT
 * resource server is wired in — callers (AuditService, the future
 * AuthorizationService) should not need to change, since they only
 * depend on the CurrentUserProvider interface.
 */
@Component
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
