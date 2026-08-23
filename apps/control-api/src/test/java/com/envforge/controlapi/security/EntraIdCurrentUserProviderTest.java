package com.envforge.controlapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.envforge.controlapi.user.Role;
import com.envforge.controlapi.user.UserEntity;
import com.envforge.controlapi.user.UserRepository;

class EntraIdCurrentUserProviderTest {

    private static final String BOOTSTRAP_ADMIN_EMAIL = "admin@envforge.local";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EntraIdCurrentUserProvider provider =
        new EntraIdCurrentUserProvider(userRepository, BOOTSTRAP_ADMIN_EMAIL);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsPersistedRoleForExistingUser() {
        setAuthentication(jwt("sub-1", "someone@envforge.dev", "Someone"));
        UserEntity existing = new UserEntity(
            UUID.randomUUID(), "sub-1", "someone@envforge.dev", "Someone",
            Role.OPERATOR, Instant.now(), Instant.now()
        );
        when(userRepository.findByExternalId("sub-1"))
            .thenReturn(Optional.of(existing));

        CurrentUser result = provider.getCurrentUser();

        assertEquals(Role.OPERATOR, result.role());
        assertEquals("someone@envforge.dev", result.email());
    }

    @Test
    void newUserMatchingBootstrapEmailGetsAdmin() {
        setAuthentication(jwt("sub-2", BOOTSTRAP_ADMIN_EMAIL, "Admin"));
        when(userRepository.findByExternalId("sub-2"))
            .thenReturn(Optional.empty());

        CurrentUser result = provider.getCurrentUser();

        assertEquals(Role.ADMIN, result.role());
    }

    @Test
    void newUserNotMatchingBootstrapEmailGetsUser() {
        setAuthentication(jwt("sub-3", "someone-else@envforge.dev", "Someone Else"));
        when(userRepository.findByExternalId("sub-3"))
            .thenReturn(Optional.empty());

        CurrentUser result = provider.getCurrentUser();

        assertEquals(Role.USER, result.role());
    }

    private static Jwt jwt(String subject, String email, String name) {
        return Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .subject(subject)
            .claim("preferred_username", email)
            .claim("name", name)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    private static void setAuthentication(Jwt jwt) {
        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken(jwt, null));
    }
}
