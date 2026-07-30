package com.envforge.controlapi.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
    @NotNull(message = "Role is required")
    Role role
) {
}
