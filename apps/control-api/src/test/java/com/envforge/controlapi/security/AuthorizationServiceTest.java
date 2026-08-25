package com.envforge.controlapi.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import com.envforge.controlapi.audit.AuditResult;
import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.user.Role;

class AuthorizationServiceTest {

    private final AuditService auditService =
        mock(AuditService.class);
    private final SecurityMetrics securityMetrics =
        mock(SecurityMetrics.class);
    private final AuthorizationService authorizationService =
        new AuthorizationService(auditService, securityMetrics);

    @Test
    void requireRoleThrowsAndRecordsAuditOnDenial() {
        CurrentUser user = new CurrentUser(
            "user-1",
            "user-1@envforge.dev",
            "User One",
            Role.USER
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireRole(
                user,
                "UPDATE_USER_ROLE",
                Role.ADMIN
            )
        );

        verify(auditService).record(
            eq(user),
            eq("UPDATE_USER_ROLE"),
            eq("AUTHORIZATION"),
            isNull(),
            eq(AuditResult.FAILURE),
            any(String.class)
        );
        verify(securityMetrics).recordForbidden();
    }

    @Test
    void requireRoleAllowsMatchingRole() {
        CurrentUser admin = new CurrentUser(
            "admin-1",
            "admin-1@envforge.dev",
            "Admin One",
            Role.ADMIN
        );

        assertDoesNotThrow(() ->
            authorizationService.requireRole(
                admin,
                "UPDATE_USER_ROLE",
                Role.ADMIN
            )
        );
    }

    @Test
    void requireOwnerOrAdminAllowsOwnerOperator() {
        CurrentUser owner = new CurrentUser(
            "operator-1",
            "owner@envforge.dev",
            "Owner One",
            Role.OPERATOR
        );

        assertDoesNotThrow(() ->
            authorizationService.requireOwnerOrAdmin(
                owner,
                "UPDATE_ENVIRONMENT",
                "owner@envforge.dev"
            )
        );
    }

    @Test
    void requireOwnerOrAdminDeniesNonOwnerOperator() {
        CurrentUser notOwner = new CurrentUser(
            "operator-2",
            "someone-else@envforge.dev",
            "Someone Else",
            Role.OPERATOR
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireOwnerOrAdmin(
                notOwner,
                "UPDATE_ENVIRONMENT",
                "owner@envforge.dev"
            )
        );
        verify(securityMetrics).recordForbidden();
    }

    @Test
    void requireOwnerOrAdminAllowsAdminEvenWhenNotOwner() {
        CurrentUser admin = new CurrentUser(
            "admin-2",
            "admin-2@envforge.dev",
            "Admin Two",
            Role.ADMIN
        );

        assertDoesNotThrow(() ->
            authorizationService.requireOwnerOrAdmin(
                admin,
                "UPDATE_ENVIRONMENT",
                "owner@envforge.dev"
            )
        );
    }

    @Test
    void requireOwnerOrAdminDeniesOwnerWithInsufficientRole() {
        CurrentUser ownerButPlainUser = new CurrentUser(
            "user-1",
            "owner@envforge.dev",
            "Owner As Plain User",
            Role.USER
        );

        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireOwnerOrAdmin(
                ownerButPlainUser,
                "UPDATE_ENVIRONMENT",
                "owner@envforge.dev"
            )
        );
        verify(securityMetrics).recordForbidden();
    }

    @Test
    void requireAdminAllowsAdminAndDeniesEveryoneElse() {
        CurrentUser admin = new CurrentUser(
            "admin-3",
            "admin-3@envforge.dev",
            "Admin Three",
            Role.ADMIN
        );
        CurrentUser operator = new CurrentUser(
            "operator-3",
            "operator-3@envforge.dev",
            "Operator Three",
            Role.OPERATOR
        );

        assertDoesNotThrow(() ->
            authorizationService.requireAdmin(admin, "UPDATE_USER_ROLE")
        );
        assertThrows(
            AccessDeniedException.class,
            () -> authorizationService.requireAdmin(
                operator,
                "UPDATE_USER_ROLE"
            )
        );
    }
}
