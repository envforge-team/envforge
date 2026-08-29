package com.envforge.controlapi.security;

import com.envforge.controlapi.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!entra")
public class DevCurrentUserProvider implements CurrentUserProvider {

    private static final String DEBUG_USER_ID_HEADER = "X-Debug-User-Id";
    private static final String DEBUG_USER_EMAIL_HEADER = "X-Debug-User-Email";
    private static final String DEBUG_USER_NAME_HEADER = "X-Debug-User-Name";
    private static final String DEBUG_USER_ROLE_HEADER = "X-Debug-User-Role";

    private static final String DEFAULT_ID = "dev-user";
    private static final String DEFAULT_EMAIL = "dev@envforge.local";
    private static final Role DEFAULT_ROLE = Role.ADMIN;

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

        String id = request.getHeader(DEBUG_USER_ID_HEADER);
        if (id == null || id.isBlank()) {
            id = DEFAULT_ID;
        }

        String displayName = request.getHeader(DEBUG_USER_NAME_HEADER);
        if (displayName == null || displayName.isBlank()) {
            displayName = email;
        }

        Role role = resolveRole(request.getHeader(DEBUG_USER_ROLE_HEADER));

        return new CurrentUser(id, email, displayName, role);
    }

    private Role resolveRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            return DEFAULT_ROLE;
        }

        try {
            return Role.valueOf(
                roleHeader.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                "Invalid X-Debug-User-Role value: " + roleHeader
            );
        }
    }
}
