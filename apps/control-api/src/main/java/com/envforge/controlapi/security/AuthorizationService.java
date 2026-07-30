package com.envforge.controlapi.security;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.envforge.controlapi.audit.AuditResult;
import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.user.Role;

@Service
public class AuthorizationService {

    private final AuditService auditService;

    public AuthorizationService(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Allows the action only if the user's role is one of allowedRoles.
     * Any denial is recorded as a FAILURE audit event before throwing.
     */
    public void requireRole(CurrentUser user, String action, Role... allowedRoles) {
        Set<Role> allowed = Set.of(allowedRoles);
        if (!allowed.contains(user.role())) {
            String message =
                "Role " + user.role() + " is not allowed to perform " + action;
            auditService.record(
                user,
                action,
                "AUTHORIZATION",
                null,
                AuditResult.FAILURE,
                message
            );
            throw new AccessDeniedException(message);
        }
    }

    public void requireAdmin(CurrentUser user, String action) {
        requireRole(user, action, Role.ADMIN);
    }

    /**
     * Allows the action if the user is ADMIN, or if the user owns the
     * resource (matched by email) and has at least OPERATOR role.
     * Meant for future use by other modules (e.g. environment update /
     * rollback), once they start depending on this service.
     */
    public void requireOwnerOrAdmin(
        CurrentUser user,
        String action,
        String resourceOwnerEmail
    ) {
        if (user.role() == Role.ADMIN) {
            return;
        }
        boolean isOwner = user.email().equalsIgnoreCase(resourceOwnerEmail);
        boolean canActOnOwnResources = user.role() == Role.OPERATOR;
        if (!isOwner || !canActOnOwnResources) {
            String message =
                "User " + user.email() + " may not perform " + action
                    + " on a resource owned by " + resourceOwnerEmail;
            auditService.record(
                user,
                action,
                "AUTHORIZATION",
                resourceOwnerEmail,
                AuditResult.FAILURE,
                message
            );
            throw new AccessDeniedException(message);
        }
    }
}
