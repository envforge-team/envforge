package com.envforge.controlapi.security;

import com.envforge.controlapi.user.Role;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * CurrentUserProvider backed by a validated Entra ID JWT (see
 * EntraSecurityConfig). Active only under the "entra" Spring profile
 * (Ziua 31).
 *
 * Role is hardcoded to USER for every authenticated principal for now -
 * real role resolution (USER / OPERATOR / ADMIN) is Ziua 32's job, once
 * app roles / group claims are wired in from Entra ID.
 */
@Component
@Profile("entra")
public class EntraIdCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser getCurrentUser() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String id = jwt.getSubject();
        String email = firstNonBlank(
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("email"),
            id
        );
        String displayName = firstNonBlank(jwt.getClaimAsString("name"), email);
        return new CurrentUser(id, email, displayName, Role.USER);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
