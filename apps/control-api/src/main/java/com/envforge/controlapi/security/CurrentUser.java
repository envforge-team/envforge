package com.envforge.controlapi.security;

import com.envforge.controlapi.user.Role;

public record CurrentUser(
    String id,
    String email,
    String displayName,
    Role role
) {
}
