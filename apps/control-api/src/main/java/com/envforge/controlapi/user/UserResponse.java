package com.envforge.controlapi.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String externalId,
    String email,
    String displayName,
    Role role,
    Instant createdAt,
    Instant updatedAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getExternalId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
