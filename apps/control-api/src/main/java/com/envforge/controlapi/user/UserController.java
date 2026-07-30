package com.envforge.controlapi.user;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.envforge.controlapi.audit.AuditResult;
import com.envforge.controlapi.audit.AuditService;
import com.envforge.controlapi.security.AuthorizationService;
import com.envforge.controlapi.security.CurrentUser;
import com.envforge.controlapi.security.CurrentUserProvider;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;

    public UserController(
        UserService userService,
        CurrentUserProvider currentUserProvider,
        AuthorizationService authorizationService,
        AuditService auditService
    ) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        UserEntity user = userService.getOrCreateCurrentUser(currentUser);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateRole(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        CurrentUser currentUser = currentUserProvider.getCurrentUser();
        authorizationService.requireAdmin(currentUser, "UPDATE_USER_ROLE");

        UserEntity updated = userService.updateRole(id, request.role());

        auditService.record(
            currentUser,
            "UPDATE_USER_ROLE",
            "USER",
            id.toString(),
            AuditResult.SUCCESS,
            "Role changed to " + request.role()
        );

        return ResponseEntity.ok(UserResponse.from(updated));
    }
}
