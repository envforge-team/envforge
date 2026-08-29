package com.envforge.controlapi.lifecycle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.security.AccessDeniedException;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.SecurityMetrics;
import com.envforge.controlapi.user.Role;
import org.junit.jupiter.api.Test;

class LifecycleOwnershipAuthorizationTest {

    private final AuthorizationService authorizationService =
        new AuthorizationService(
            mock(AuditService.class),
            mock(SecurityMetrics.class)
        );

    @Test
    void operatorShouldMatchStableOwnerId() {
        CurrentUser operator = new CurrentUser(
            "owner-id",
            "owner@example.test",
            "Owner",
            Role.OPERATOR
        );

        assertDoesNotThrow(
            () -> authorizationService.requireOwnerOrAdmin(
                operator,
                "DELETE_ENVIRONMENT",
                "owner-id"
            )
        );
    }

    @Test
    void nonOwnerOperatorShouldBeDenied() {
        CurrentUser operator = new CurrentUser(
            "different-id",
            "other@example.test",
            "Other",
            Role.OPERATOR
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireOwnerOrAdmin(
                operator,
                "DELETE_ENVIRONMENT",
                "owner-id"
            )
        );
    }
}
