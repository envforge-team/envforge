package com.envforge.controlapi.security;

import com.envforge.controlapi.user.Role;
import com.envforge.controlapi.user.UserEntity;
import com.envforge.controlapi.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * CurrentUserProvider backed by a validated Entra ID JWT (see
 * EntraSecurityConfig). Active only under the "entra" Spring profile
 * (Ziua 31).
 *
 * Role resolution (Ziua 32):
 * - if the user already exists in our database (looked up by external id
 *   = JWT subject claim), the persisted role is authoritative. This is
 *   what makes AuthorizationService.requireRole / requireAdmin actually
 *   mean something once an admin promotes someone via
 *   PUT /api/users/{id}/role - without this DB lookup, every request
 *   would just get whatever default role this provider hands out, and
 *   role changes would never be visible to authorization checks.
 * - if the user does not exist yet (first-ever login), the default role
 *   is USER, UNLESS their email matches
 *   envforge.security.bootstrap-admin-email (see
 *   application-entra.properties), in which case they get ADMIN on first
 *   login. Without this, nobody could ever reach the ADMIN-only
 *   role-update endpoint to promote the very first admin.
 */
@Component
@Profile("entra")
public class EntraIdCurrentUserProvider implements CurrentUserProvider {

    private final UserRepository userRepository;
    private final String bootstrapAdminEmail;

    public EntraIdCurrentUserProvider(
        UserRepository userRepository,
        @Value("${envforge.security.bootstrap-admin-email:}") String bootstrapAdminEmail
    ) {
        this.userRepository = userRepository;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

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
        Role role = userRepository.findByExternalId(id)
            .map(UserEntity::getRole)
            .orElseGet(() -> defaultRoleFor(email));
        return new CurrentUser(id, email, displayName, role);
    }

    private Role defaultRoleFor(String email) {
        if (email != null && !bootstrapAdminEmail.isBlank()
            && bootstrapAdminEmail.equalsIgnoreCase(email)) {
            return Role.ADMIN;
        }
        return Role.USER;
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
