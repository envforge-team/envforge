package com.envforge.controlapi.security;

import com.envforge.controlapi.audit.AuditResult;
import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.user.Role;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final AuditService auditService;
    private final SecurityMetrics securityMetrics;

    public AuthorizationService(
        AuditService auditService,
        SecurityMetrics securityMetrics
    ) {
        this.auditService = auditService;
        this.securityMetrics = securityMetrics;
    }

    public void requireRole(
        CurrentUser user,
        String action,
        Role... allowedRoles
    ) {
        Set<Role> allowed = Set.of(allowedRoles);

        if (!allowed.contains(user.role())) {
            String message =
                "Role " + user.role()
                    + " is not allowed to perform "
                    + action;

            auditService.record(
                user,
                action,
                "AUTHORIZATION",
                null,
                AuditResult.FAILURE,
                message
            );

            securityMetrics.recordForbidden();

            throw new AccessDeniedException(message);
        }
    }

    public void requireAdmin(
        CurrentUser user,
        String action
    ) {
        requireRole(user, action, Role.ADMIN);
    }

    public void requireOwnerOrAdmin(
        CurrentUser user,
        String action,
        String resourceOwner
    ) {
        if (user.role() == Role.ADMIN) {
            return;
        }

        boolean isOwner =
            matches(user.id(), resourceOwner)
                || matches(user.email(), resourceOwner);

        boolean canActOnOwnResources =
            user.role() == Role.OPERATOR;

        if (!isOwner || !canActOnOwnResources) {
            String message =
                "User " + user.email()
                    + " may not perform "
                    + action
                    + " on a resource owned by "
                    + resourceOwner;

            auditService.record(
                user,
                action,
                "AUTHORIZATION",
                resourceOwner,
                AuditResult.FAILURE,
                message
            );

            securityMetrics.recordForbidden();

            throw new AccessDeniedException(message);
        }
    }

    private boolean matches(
        String userValue,
        String resourceOwner
    ) {
        return userValue != null
            && resourceOwner != null
            && userValue.equalsIgnoreCase(resourceOwner);
    }
}
