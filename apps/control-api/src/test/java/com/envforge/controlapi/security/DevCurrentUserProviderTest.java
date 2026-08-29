package com.envforge.controlapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.envforge.controlapi.user.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class DevCurrentUserProviderTest {

    @Test
    void shouldReadDebugIdentityAndRole() {
        HttpServletRequest request =
            mock(HttpServletRequest.class);

        when(request.getHeader("X-Debug-User-Id"))
            .thenReturn("owner-user");
        when(request.getHeader("X-Debug-User-Email"))
            .thenReturn("owner@example.test");
        when(request.getHeader("X-Debug-User-Name"))
            .thenReturn("Owner");
        when(request.getHeader("X-Debug-User-Role"))
            .thenReturn("operator");

        CurrentUser user =
            new DevCurrentUserProvider(request)
                .getCurrentUser();

        assertThat(user.id())
            .isEqualTo("owner-user");
        assertThat(user.email())
            .isEqualTo("owner@example.test");
        assertThat(user.displayName())
            .isEqualTo("Owner");
        assertThat(user.role())
            .isEqualTo(Role.OPERATOR);
    }

    @Test
    void shouldDefaultToAdminForLocalDevelopment() {
        HttpServletRequest request =
            mock(HttpServletRequest.class);

        CurrentUser user =
            new DevCurrentUserProvider(request)
                .getCurrentUser();

        assertThat(user.id())
            .isEqualTo("dev-user");
        assertThat(user.email())
            .isEqualTo("dev@envforge.local");
        assertThat(user.role())
            .isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldRejectInvalidDebugRole() {
        HttpServletRequest request =
            mock(HttpServletRequest.class);

        when(request.getHeader("X-Debug-User-Role"))
            .thenReturn("SUPERUSER");

        assertThatThrownBy(
            () -> new DevCurrentUserProvider(request)
                .getCurrentUser()
        )
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining(
                "Invalid X-Debug-User-Role"
            );
    }
}
